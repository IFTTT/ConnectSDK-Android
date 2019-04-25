package com.ifttt.ui;

/**
 * Interface that represents an action that can be reverted.
 */
interface Revertable {

    void run();

    /**
     * Represents an action that reverts the action done by the {@link #run()} function.
     */
    void revert();
}
