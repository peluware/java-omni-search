package com.peluware.omnisearch.reactive.mutiny;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import io.smallrye.mutiny.Uni;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

@UtilityClass
public final class MutinyPageUtils {

    public static <T> Uni<Page<T>> deferred(
            @NotNull Uni<List<T>> contentUni,
            Pagination pagination,
            Sort sort,
            @NotNull Supplier<Uni<Long>> totalElementsUniSupplier
    ) {
        return contentUni.onItem().transformToUni(content -> {
            if (pagination == null || !pagination.isPaginated()) {
                return Uni.createFrom().item(new Page<>(content, pagination, sort, content.size()));
            }

            if (pagination.getSize() > content.size()) {
                if (pagination.getOffset() == 0) {
                    return Uni.createFrom().item(new Page<>(content, pagination, sort, content.size()));
                }
                if (!content.isEmpty()) {
                    return totalElementsUniSupplier.get()
                            .onItem().transform(total -> new Page<>(content, pagination, sort, total));
                }
            }

            return totalElementsUniSupplier.get()
                    .onItem().transform(total -> new Page<>(content, pagination, sort, total));
        });

    }
}
