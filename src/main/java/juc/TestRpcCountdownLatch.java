package juc;

import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.*;

public class TestRpcCountdownLatch {


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("begin");
        ExecutorService service = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(4);
        Future<Map<String,Object>> f1 = service.submit(() -> {
            Map<String, Object> r =
                    restTemplate.getForObject("http://localhost:8080/order/{1}", Map.class, 1);
            return r;
        });
        Future<Map<String, Object>> f2 = service.submit(() -> {
            Map<String, Object> r =
                    restTemplate.getForObject("http://localhost:8080/product/{1}", Map.class, 1);
            return r;
        });
        Future<Map<String, Object>> f3 = service.submit(() -> {
            Map<String, Object> r =
                    restTemplate.getForObject("http://localhost:8080/product/{1}", Map.class, 2);
            return r;
        });

        Future<Map<String, Object>> f4 = service.submit(() -> {
            Map<String, Object> r =
                    restTemplate.getForObject("http://localhost:8080/logistics/{1}", Map.class, 1);
            return r;
        });
        System.out.println(f1.get());
        System.out.println(f2.get());
        System.out.println(f3.get());
        System.out.println(f4.get());
        System.out.println("执行完毕");
        service.shutdown();
    }
}
