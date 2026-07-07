package com.rc.readcompassbatch.dto;

import java.util.UUID;

public record CountMismatchDetail(
        String type,
        UUID targetId,
        Integer savedCount,
        Integer actualCount
) {
}