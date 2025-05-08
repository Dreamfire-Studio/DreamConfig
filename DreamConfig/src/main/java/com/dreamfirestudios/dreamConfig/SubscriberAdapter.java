package com.dreamfirestudios.dreamConfig;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.function.Consumer;

public class SubscriberAdapter<T> implements org.reactivestreams.Subscriber<T> {
    private final Consumer<T> onNext;
    private final Consumer<Throwable> onError;
    private final Runnable onComplete;
    private boolean calledOnNext = false;

    public SubscriberAdapter(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void onSubscribe(org.reactivestreams.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        calledOnNext = true;
        onNext.accept(item);
    }

    @Override
    public void onError(Throwable throwable) {
        onError.accept(throwable);
    }

    @Override
    public void onComplete() {
        if (!calledOnNext) {
            onComplete.run();
        }
    }
}
