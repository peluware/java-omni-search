package com.peluware.omnisearch.reactive;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Supplier;


public class FlowPageUtils {

    private FlowPageUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> Flow.Publisher<Page<T>> deferred(
            Flow.Publisher<List<T>> contentPublisher,
            Pagination pagination,
            Sort sort,
            Supplier<? extends Flow.Publisher<Long>> totalElementsPublisherSupplier
    ) {
        return subscriber -> contentPublisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscriber.onSubscribe(subscription);
                subscription.request(1); // esperamos una lista
            }

            @Override
            public void onNext(List<T> content) {
                try {
                    // Caso: unpaginated
                    if (pagination == null || !pagination.isPaginated()) {
                        subscriber.onNext(new Page<>(content, pagination, sort, content.size()));
                        subscriber.onComplete();
                        return;
                    }

                    // Caso: primera página y content.size() < pageSize
                    if (pagination.getSize() > content.size() && pagination.getOffset() == 0) {
                        subscriber.onNext(new Page<>(content, pagination, sort, content.size()));
                        subscriber.onComplete();
                        return;
                    }

                    // Caso: necesitamos totalElements
                    totalElementsPublisherSupplier.get().subscribe(new Flow.Subscriber<>() {
                        private Flow.Subscription innerSub;

                        @Override
                        public void onSubscribe(Flow.Subscription innerSub) {
                            this.innerSub = innerSub;
                            innerSub.request(1); // solo necesitamos un valor
                        }

                        @Override
                        public void onNext(Long total) {
                            subscriber.onNext(new Page<>(content, pagination, sort, total));
                            subscriber.onComplete();
                            innerSub.cancel();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            subscriber.onError(throwable);
                        }

                        @Override
                        public void onComplete() {
                            // noop, ya cerramos en onNext
                        }
                    });
                } finally {
                    subscription.cancel(); // dejamos de escuchar más listas
                }
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                // noop, el cierre lo manejamos tras emitir Page
            }
        });
    }


}
