/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.scheduler.timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.SchedulerConstants;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.SchedulerUtil;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.TaskFactory;
import org.openmrs.scheduler.db.SchedulerDAO;
import org.openmrs.util.InsertedOrderComparator;
import org.openmrs.util.OpenmrsMemento;
import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * Simple scheduler service that uses JDK timer to trigger and execute scheduled tasks.
 */
public class TimerSchedulerServiceImpl implements SchedulerService {
	
	/**
	 * Logger
	 */
	private static Log log = LogFactory.getLog(TimerSchedulerServiceImpl.class);
	
	/**
	 * Registered task list
	 */
	private Set<TaskDefinition> registeredTasks = new HashSet<TaskDefinition>();
	
	/**
	 * Scheduled Task Map
	 */
	private static Map<Integer, TimerSchedulerTask> scheduledTasks = new HashMap<Integer, TimerSchedulerTask>();
	
	/**
	 * A single timer used to keep track of all scheduled tasks. The Timer's associated thread
	 * should run as a daemon. A deamon thread is called for if the timer will be used to schedule
	 * repeating "maintenance activities", which must be performed as long as the application is
	 * running, but should not prolong the lifetime of the application.
	 * 
	 * @see java.util.Timer#Timer(boolean)
	 */
	private Timer timerScheduler = new Timer(true);
	
	/**
	 * Global data access object context
	 */
	private SchedulerDAO schedulerDAO;
	
	/**
	 * Gets the scheduler data access object.
	 * 
	 * @return
	 */
	public SchedulerDAO getSchedulerDAO() {
		return this.schedulerDAO;
	}
	
	/**
	 * Sets the scheduler data access object.
	 */
	public void setSchedulerDAO(SchedulerDAO dao) {
		this.schedulerDAO = dao;
	}
	
	/**
	 * Start up hook for the scheduler and all of its scheduled tasks.
	 */
	public void onStartup() {
		log.debug("Starting scheduler service ...");
		
		// Get all of the tasks in the database
		Collection<TaskDefinition> taskDefinitions = getSchedulerDAO().getTasks();
		
		// Iterate through the tasks and start them if their startOnStartup flag is true
		if (taskDefinitions != null) {
			for (TaskDefinition taskDefinition : taskDefinitions) {
				try {
					// If the task is configured to start on startup, we schedule it to run
					// Otherwise it needs to be started manually.
					if (taskDefinition.getStartOnStartup()) {
						scheduleTask(taskDefinition);
					}
					
				}
				catch (Throwable t) {
					log.error("Failed to schedule task for class " + taskDefinition.getTaskClass(), t);
				}
			}
		}
	}
	
	/**
	 * Shutdown hook for the scheduler and all of its scheduled tasks.
	 */
	public void onShutdown() {
		log.debug("Gracefully shutting down scheduler service ...");
		// gracefully shutdown all tasks and remove all references to the timers, scheduler
		try {
			shutdownAllTasks();
			timerScheduler.cancel(); // Just a precaution - this shouldn't be necessary if shutdownAllTasks() does its job
		}
		catch (APIException e) {
			log.error("Failed to stop all tasks due to API exception", e);
		}
		finally {
			scheduledTasks = null;
		}
		
	}
	
	/**
	 * Shutdown all running tasks.
	 */
	public void shutdownAllTasks() {
		
		// iterate over this (copied) list of tasks and stop them all
		for (TaskDefinition task : getScheduledTasks()) {
			try {
				
				shutdownTask(task);
				
			}
			catch (SchedulerException e) {
				log.error("Failed to stop task " + task.getTaskClass() + " due to Scheduler exception", e);
			}
			catch (APIException e) {
				log.error("Failed to stop task " + task.getTaskClass() + " due to API exception", e);
			}
		}
	}
	
	/**
	 * Schedule the given task according to the given schedule.
	 * 
	 * @param task the task to be scheduled
	 * @param schedule the time and interval for the scheduled task
	 */
	public Task scheduleTask(TaskDefinition taskDefinition) throws SchedulerException {
		Task clientTask = null;
		if (taskDefinition != null) {
			
			// Cancel any existing timer tasks for the same task definition
			// TODO Make sure this is the desired behavior 
			// TODO Do we ever want the same task definition to run more than once?
			TimerSchedulerTask schedulerTask = scheduledTasks.get(taskDefinition.getId());
			if (schedulerTask != null) {
				//schedulerTask.cancel();					
				log.info("Shutting down the existing instance of this task to avoid conflicts!!");
				schedulerTask.shutdown();
			}
			
			try {
				
				// Create new task from task definition 
				clientTask = TaskFactory.getInstance().createInstance(taskDefinition);
				
				// if we were unable to get a class, just quit
				if (clientTask != null) {
					
					schedulerTask = new TimerSchedulerTask(clientTask);
					
					taskDefinition.setTaskInstance(clientTask);
					
					// Once this method is called, the timer is set to start at the given start time.
					// NOTE:  We need to adjust the repeat interval as the JDK Timer expects time in milliseconds and 
					// we record by seconds.  
					
					long repeatInterval = taskDefinition.getRepeatInterval()
					        * SchedulerConstants.SCHEDULER_MILLIS_PER_SECOND;
					
					if (taskDefinition.getStartTime() != null) {
						// Need to calculate the "next execution time" because the scheduled time is most likely in the past
						// and the JDK timer will run the task X number of times from the start time until now to catch up.
						Date nextTime = SchedulerUtil.getNextExecution(taskDefinition);
						
						// Start task at fixed rate at given future date and repeat as directed 							
						log.info("Starting task ... the task will execute for the first time at " + nextTime);
						
						// Schedule the task to run at a fixed rate
						timerScheduler.scheduleAtFixedRate(schedulerTask, nextTime, repeatInterval);
					} else if (repeatInterval > 0) {
						// Start task on repeating schedule, delay for SCHEDULER_DEFAULT_DELAY seconds	
						log.info("Delaying start time by " + SchedulerConstants.SCHEDULER_DEFAULT_DELAY + " seconds");
						timerScheduler.scheduleAtFixedRate(schedulerTask, SchedulerConstants.SCHEDULER_DEFAULT_DELAY,
						    repeatInterval);
					} else {
						// schedule for single execution, starting now
						log.info("Starting one-shot task");
						timerScheduler.schedule(schedulerTask, new Date());
					}
					
					// Update task that has been started
					log.debug("Registering timer for task " + taskDefinition.getId());
					
					//  Add the new timer to the scheduler running task list  
					scheduledTasks.put(taskDefinition.getId(), schedulerTask);
					
					// Update the timer status in the database
					taskDefinition.setStarted(true);
					saveTask(taskDefinition);
				}
			}
			catch (Exception e) {
				log.error("Failed to schedule task " + taskDefinition.getName(), e);
				throw new SchedulerException(e);
			}
		}
		return clientTask;
	}
	
	/**
	 * Stops a running task.
	 * 
	 * @param task the task to be stopped
	 * @see org.openmrs.scheduler.SchedulerService#stopTask(org.openmrs.scheduler.TaskConfig)
	 */
	public void shutdownTask(TaskDefinition taskDefinition) throws SchedulerException {
		if (taskDefinition != null) {
			
			// Remove the task from the scheduled tasks and shutdown the timer
			TimerSchedulerTask schedulerTask = scheduledTasks.remove(taskDefinition.getId());
			if (schedulerTask != null) {
				schedulerTask.shutdown(); // Stops the timer and tells the timer task to release its resources 
			}
			
			// Update task that has been started
			taskDefinition.setStarted(false);
			saveTask(taskDefinition);
		}
	}
	
	/**
	 * Loop over all currently started tasks and cycle them. This should be done after the
	 * classloader has been changed (e.g. during module start/stop)
	 */
	public void rescheduleAllTasks() throws SchedulerException {
		for (TaskDefinition task : getScheduledTasks()) {
			try {
				rescheduleTask(task);
			}
			catch (SchedulerException e) {
				log.error("Failed to restart task: " + task.getName(), e);
			}
		}
	}
	
	/**
	 * @see org.openmrs.scheduler.SchedulerService#rescheduleTask(org.openmrs.scheduler.TaskDefinition)
	 */
	public Task rescheduleTask(TaskDefinition taskDefinition) throws SchedulerException {
		shutdownTask(taskDefinition);
		return scheduleTask(taskDefinition);
	}
	
	/**
	 * Register a new task by adding it to our task map with an empty schedule map.
	 * 
	 * @param definition task to register
	 */
	public void registerTask(TaskDefinition definition) {
		registeredTasks.add(definition);
	}
	
	/**
	 * Get all scheduled tasks.
	 * 
	 * @return all scheduled tasks
	 */
	public Collection<TaskDefinition> getScheduledTasks() {
		// The real list of scheduled tasks is kept up-to-date in the scheduledTasks map
		// TODO change the index for the scheduledTasks map to be the TaskDefinition rather than the ID
		List<TaskDefinition> list = new ArrayList<TaskDefinition>();
		Set<Integer> taskIds = scheduledTasks.keySet();
		for (Integer id : taskIds) {
			TaskDefinition task = getTask(id);
			log.debug("Adding scheduled task " + id + " to list (" + task.getRepeatInterval() + ")");
			list.add(task);
		}
		return list;
		
	}
	
	/**
	 * Get all registered tasks.
	 * 
	 * @return all registerd tasks
	 */
	public Collection<TaskDefinition> getRegisteredTasks() {
		return getSchedulerDAO().getTasks();
	}
	
	/**
	 * Get the task with the given identifier.
	 * 
	 * @param id the identifier of the task
	 */
	public TaskDefinition getTask(Integer id) {
		if (log.isDebugEnabled())
			log.debug("get task " + id);
		return getSchedulerDAO().getTask(id);
	}
	
	/**
	 * Get the task with the given name.
	 * 
	 * @param name name of the task
	 */
	public TaskDefinition getTaskByName(String name) {
		if (log.isDebugEnabled())
			log.debug("get task " + name);
		TaskDefinition foundTask = null;
		try {
			foundTask = getSchedulerDAO().getTaskByName(name);
		}
		catch (ObjectRetrievalFailureException orfe) {
			log.warn("getTaskByName(" + name + ") failed, because: " + orfe);
		}
		return foundTask;
	}
	
	/**
	 * Get the task with the given identifier.
	 * 
	 * @param id the identifier of the task
	 */
	public void saveTask(TaskDefinition task) {
		if (task.getId() != null) {
			setChangedMetadata(task);
			getSchedulerDAO().updateTask(task);
		} else {
			setCreatedMetadata(task);
			getSchedulerDAO().createTask(task);
		}
	}
	
	/**
	 * Delete the task with the given identifier.
	 * 
	 * @param id the identifier of the task
	 */
	public void deleteTask(Integer id) {
		
		// try to stop the task (ignore errors)
		TaskDefinition task = getTask(id);
		if (task.getStarted()) {
			try {
				shutdownTask(task);
			}
			catch (SchedulerException e) {
				log.error("Failed to remove task " + task.getName(), e);
			}
		}
		
		// delete the task
		getSchedulerDAO().deleteTask(id);
	}
	
	/**
	 * Convenience method for setting all metadata fields.
	 * 
	 * @param task
	 */
	public void setCreatedMetadata(TaskDefinition task) {
		if (task.getCreatedBy() == null) {
			task.setCreatedBy(Context.getAuthenticatedUser());
		}
		if (task.getDateCreated() == null) {
			task.setDateCreated(new Date());
		}
		setChangedMetadata(task);
	}
	
	/**
	 * Convenience method for setting the changed by and changed date fields
	 * 
	 * @param task
	 */
	public void setChangedMetadata(TaskDefinition task) {
		task.setChangedBy(Context.getAuthenticatedUser());
		task.setDateChanged(new Date());
	}
	
	/**
	 * Get system variables.
	 */
	public SortedMap<String, String> getSystemVariables() {
		TreeMap<String, String> systemVariables = new TreeMap<String, String>(new InsertedOrderComparator());
		// scheduler username and password can be found in the global properties
		// TODO Look into java.util.concurrent.TimeUnit class.  
		// TODO Remove this from global properties.  This is a constant value that should never change.  
		systemVariables.put("SCHEDULER_MILLIS_PER_SECOND", String.valueOf(SchedulerConstants.SCHEDULER_MILLIS_PER_SECOND));
		return systemVariables;
	}
	
	/**
	 * Saves and stops all active tasks
	 * 
	 * @returns OpenmrsMemento
	 */
	public OpenmrsMemento saveToMemento() {
		
		Set<TaskDefinition> tasks = new HashSet<TaskDefinition>();
		
		for (TaskDefinition task : getScheduledTasks()) {
			tasks.add(task);
			try {
				shutdownTask(task);
			}
			catch (SchedulerException e) {
				// just swallow exceptions
				log.debug("Failed to stop task while saving memento " + task.getName(), e);
			}
		}
		
		TimerSchedulerMemento memento = new TimerSchedulerMemento(tasks);
		memento.saveErrorTasks();
		
		return memento;
	}
	
	/**
	 * 
	 */
	public void restoreFromMemento(OpenmrsMemento memento) {
		
		if (memento != null && memento instanceof TimerSchedulerMemento) {
			TimerSchedulerMemento timerMemento = (TimerSchedulerMemento) memento;
			
			Set<TaskDefinition> tasks = (HashSet<TaskDefinition>) timerMemento.getState();
			
			// try to start all of the tasks that were stopped right before this restore
			for (TaskDefinition task : tasks) {
				try {
					scheduleTask(task);
				}
				catch (Exception e) {
					// essentially swallow exceptions
					log.debug("EXPECTED ERROR IF STOPPING THIS TASK'S MODULE: Unable to start task " + task, e);
					
					// save this errored task and try again next time we restore
					timerMemento.addErrorTask(task);
				}
			}
		}
	}
	
	/**
	 * @see org.openmrs.scheduler.SchedulerService#getStatus(java.lang.Integer) TODO
	 *      internationalization of string status messages
	 */
	public String getStatus(Integer id) {
		
		// Get the scheduled timer task
		TimerSchedulerTask scheduledTask = scheduledTasks.get(id);
		
		if (scheduledTask != null) {
			if (scheduledTask.scheduledExecutionTime() > 0) {
				return "Scheduled to execute at " + new Date(scheduledTask.scheduledExecutionTime());
			} else {
				return "Currently executing";
			}
		}
		return "Not Running";
	}
	
}
