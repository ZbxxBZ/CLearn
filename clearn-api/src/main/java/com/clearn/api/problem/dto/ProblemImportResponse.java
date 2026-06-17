package com.clearn.api.problem.dto;

import java.util.List;

public record ProblemImportResponse(
        int importedCount,
        List<Long> problemIds
) {
}
