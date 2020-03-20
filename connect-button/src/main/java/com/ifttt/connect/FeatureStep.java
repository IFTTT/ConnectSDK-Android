package com.ifttt.connect;

/**
 * Data structure representing the 3 basic feature "steps": triggers, queries and actions. The 3 types of steps are
 * basic components that provides different functionality from a service.
 */
public final class FeatureStep {

    /**
     * Enum representation used to identify the type of the step.
     */
    public enum StepType {
        /**
         * Trigger step type, more details see https://platform.ifttt.com/docs/connect_api#triggers
         */
        Trigger,
        /**
         * Query step type, more details see https://platform.ifttt.com/docs/connect_api#actions
         */
        Action,
        /**
         * Action step type, more details see https://platform.ifttt.com/docs/connect_api#queries
         */
        Query
    }

    public final StepType stepType;

    /**
     * User-friendly name for the feature step.
     */
    public final String label;

    /**
     * Unique identifier for the feature step. Between two different features, this id is unique even if they are from
     * the same step (trigger/query/action).
     */
    public final String id;

    /**
     * Unique identifier for the step. Different from {@link #id}, this id is from the trigger, query or action, which
     * can be the same if two features have the same steps.
     */
    public final String stepId;

    /**
     * Unique identifier for the service. If the steps (triggers/queries/actions) are from the same service, this value
     * is going to be the same among them.
     */
    public final String serviceId;

    public FeatureStep(StepType stepType, String label, String id, String stepId, String serviceId) {
        this.stepType = stepType;
        this.label = label;
        this.id = id;
        this.stepId = stepId;
        this.serviceId = serviceId;
    }
}
