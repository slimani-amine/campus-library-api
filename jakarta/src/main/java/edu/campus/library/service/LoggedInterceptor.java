package edu.campus.library.service;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.logging.Logger;

@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggedInterceptor {

    private static final Logger LOG = Logger.getLogger(LoggedInterceptor.class.getName());

    @AroundInvoke
    public Object log(InvocationContext context) throws Exception {
        LOG.info("-> " + context.getMethod().getDeclaringClass().getSimpleName()
                + "." + context.getMethod().getName());
        return context.proceed();
    }
}
