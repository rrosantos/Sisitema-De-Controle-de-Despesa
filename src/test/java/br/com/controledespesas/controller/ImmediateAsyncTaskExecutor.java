package br.com.controledespesas.controller;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

final class ImmediateAsyncTaskExecutor implements AsyncTaskExecutor {

    @Override
    public <T> void execute(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError, Runnable onFinished) {
        try {
            T result = task.call();
            if (onFinished != null) {
                onFinished.run();
            }
            onSuccess.accept(result);
        } catch (Throwable throwable) {
            if (onFinished != null) {
                onFinished.run();
            }
            onError.accept(throwable);
        }
    }
}
