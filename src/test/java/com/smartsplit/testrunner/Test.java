package com.smartsplit.testrunner;

import java.lang.annotation.*;

/** Marks a test method. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {}
