package org.camunda.bpm.extension.graphql.resolvers;

import com.coxautodev.graphql.tools.GraphQLRootResolver;
import org.camunda.bpm.application.ProcessApplicationContext;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.identity.*;
import org.camunda.bpm.extension.graphql.types.KeyValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;


@Component
public class Query implements GraphQLRootResolver {

    @Autowired
    ProcessEngine processEngine;

    @Autowired
    TaskService taskService;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    IdentityService identityService;

    @Autowired
    ProcessEngineConfigurationImpl processEngineConfiguration;

    public Query() {
    }

    public List<Task> tasks(String assignee, String name, String nameLike) {
        TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery = (assignee != null) ? taskQuery.taskAssignee(assignee):taskQuery;
        taskQuery = (name != null) ? taskQuery.taskName(name):taskQuery;
        taskQuery = (nameLike != null) ? taskQuery.taskNameLike(nameLike):taskQuery;
        taskQuery.initializeFormKeys();
        List<Task> tasks = taskQuery.list();
        return tasks;
    }

    public Task task(String id) {
        TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery = taskQuery.taskId(id);
        taskQuery.initializeFormKeys();
        Task task = taskQuery.singleResult();
        return task;
    }

    public List<ProcessInstance> processInstances(String businessKey) {
    	ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();
    	processInstanceQuery = (businessKey != null) ? processInstanceQuery.processInstanceBusinessKey(businessKey):processInstanceQuery;
        List<ProcessInstance> processInstances = processInstanceQuery.list();
        return processInstances;
    }

    public List<ProcessDefinition> processDefinitions(Boolean isSuspended, Boolean latest) {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        processDefinitionQuery = (isSuspended != null && isSuspended == true) ? processDefinitionQuery.suspended() : processDefinitionQuery;
        processDefinitionQuery = (latest != null && latest == true) ? processDefinitionQuery.latestVersion() : processDefinitionQuery;

        List <ProcessDefinition> processDefinitionList = processDefinitionQuery.list();
        return processDefinitionList;
    }

    public ProcessDefinition processDefinition(String id) {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        ProcessDefinition processDefinition = processDefinitionQuery.processDefinitionId(id).singleResult();

        return processDefinition;
    }

    public List<KeyValuePair> taskVariables(String taskId, Collection<String> names ) {
        List<KeyValuePair> keyValuePairs;

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String pdid = task.getProcessDefinitionId();
        if (pdid == null)
            return null;

        try {
            Util.switchContext(repositoryService, pdid, processEngineConfiguration);
            VariableMap variableMap = taskService.getVariablesTyped(taskId, names, true);
            keyValuePairs = Util.getKeyValuePairs(variableMap);

        } finally {
            ProcessApplicationContext.clear();
        }

        return keyValuePairs;
    }

    public List<Group> groups(String groupName, String groupNameLike, String groupType) {
        GroupQuery query = identityService.createGroupQuery();
        query = (groupName != null)      ? query.groupName(groupName)          : query;
        query = (groupNameLike != null)  ? query.groupNameLike(groupNameLike)  : query;
        query = (groupType != null)      ? query.groupType(groupType)          : query;
        List<Group> groups = query.list();
        return groups;
    }

    public Group group(String groupId) {
        if(groupId != null) {
            Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
            return group;
        } else {
            return null;
        }
    }

    public List<User> users(String firstName, String firstNameLike, String groupId){
        UserQuery query = identityService.createUserQuery();
        query = (firstName != null)      ? query.userFirstName(firstName)           : query;
        query = (firstNameLike != null)  ? query.userFirstNameLike(firstNameLike)   : query;
        query = (groupId != null)  ? query.memberOfGroup(groupId)       : query;
        List<User> users = query.list();

        return users;
    }

    public User user(String userId) {
        if(userId != null) {
            User user = identityService.createUserQuery().userId(userId).singleResult();
            return user;
        } else {
            return null;
        }
    }
}
