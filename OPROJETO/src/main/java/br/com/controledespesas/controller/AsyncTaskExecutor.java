package br.com.controledespesas.controller;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface AsyncTaskExecutor {

    <T> void execute(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError, Runnable onFinished);
}
