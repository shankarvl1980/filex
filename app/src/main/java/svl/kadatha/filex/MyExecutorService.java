package svl.kadatha.filex;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyExecutorService {

    private static ExecutorService executorService;

    public synchronized static ExecutorService getExecutorService()
    {
        if(executorService==null)
        {
            executorService=Executors.newCachedThreadPool();
        }
        return executorService;
    }

}
