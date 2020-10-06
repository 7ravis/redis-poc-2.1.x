package com.example.redispoc.service;

/**
 * This interface merely exists to aid Spring configuration; there are not
 * common methods that need to be implemented because event workers in this
 * project independently configure jobs for Spring's task framework.
 */
public interface EventWorker {

}
