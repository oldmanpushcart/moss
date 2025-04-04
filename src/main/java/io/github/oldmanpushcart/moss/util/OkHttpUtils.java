package io.github.oldmanpushcart.moss.util;

import jakarta.validation.constraints.NotNull;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OkHttpUtils {

    public static CompletionStage<Response> async(OkHttpClient http, Request request) {
        final var future = new ResponseCompletableFutureCallback();
        http.newCall(request).enqueue(future);
        return future;
    }

    public static class ResponseCompletableFutureCallback
            extends CompletableFuture<Response>
            implements okhttp3.Callback {

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            completeExceptionally(e);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
            complete(response);
        }

    }

    public static CompletionStage<String> thenComposeStringBody(Response response) {
        final var completed = new CompletableFuture<String>();
        try {
            final var stringBody = Objects.requireNonNull(response.body()).string();
            completed.complete(stringBody);
        } catch (IOException e) {
            completed.completeExceptionally(e);
        }
        return completed;
    }

}
