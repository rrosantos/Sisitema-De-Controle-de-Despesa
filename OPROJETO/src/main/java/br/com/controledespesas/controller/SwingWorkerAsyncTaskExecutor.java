package br.com.controledespesas.controller;

import javax.swing.SwingWorker;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Executa tarefas em segundo plano usando SwingWorker e devolve os callbacks para a interface.
 */
public class SwingWorkerAsyncTaskExecutor implements AsyncTaskExecutor {

    @Override
    public <T> void execute(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError, Runnable onFinished) {
        Objects.requireNonNull(task, "task nao pode ser nula.");
        Objects.requireNonNull(onSuccess, "onSuccess nao pode ser nulo.");
        Objects.requireNonNull(onError, "onError nao pode ser nulo.");

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                T result = null;
                Throwable failure = null;

                try {
                    result = get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failure = exception;
                } catch (ExecutionException exception) {
                    failure = exception.getCause() != null ? exception.getCause() : exception;
                }

                if (onFinished != null) {
                    onFinished.run();
                }

                if (failure == null) {
                    onSuccess.accept(result);
                } else {
                    onError.accept(failure);
                }
            }
        }.execute();
    }
}
