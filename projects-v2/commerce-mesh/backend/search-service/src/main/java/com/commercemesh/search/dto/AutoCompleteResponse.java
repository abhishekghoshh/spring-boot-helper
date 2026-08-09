package com.commercemesh.search.dto;

import java.util.List;

public record AutoCompleteResponse(
        List<String> suggestions
) {
}
