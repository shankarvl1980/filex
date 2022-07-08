package svl.kadatha.filex;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyExecutorService {

    private static ExecutorService executorService;

    public static ExecutorService getExecutorService()
    {
        if(executorService==null)
        {
            executorService=Executors.newCachedThreadPool();
        }
        return executorService;
    }

}
