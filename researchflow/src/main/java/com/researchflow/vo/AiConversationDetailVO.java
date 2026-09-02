package com.researchflow.vo;

import java.util.List;

public record AiConversationDetailVO(
        AiConversationVO conversation,
        List<AiPersistedMessageVO> messages
) {
}
