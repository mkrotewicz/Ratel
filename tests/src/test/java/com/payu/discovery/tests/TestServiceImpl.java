package com.payu.discovery.tests;

import com.payu.discovery.Publish;

@Publish
public class TestServiceImpl implements TestService {

    private int counter = 0;

    @Override
    public int testMethod() {
        return ++counter;
    }
}
