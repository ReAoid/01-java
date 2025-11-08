package com.chatbot.mapper;

import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.dto.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * ChatMessage领域模型与WebSocket DTO的转换器
 * 
 * 职责：
 * - 将领域模型转换为DTO（用于发送给前端）
 * - 将DTO转换为领域模型（用于接收前端消息）
 * - 处理数据格式转换（如Base64编码）
 * 
 * 设计原则：
 * - 单一职责：只负责数据转换
 * - 无业务逻辑：不做复杂计算
 * - 空值安全：优雅处理null值
 */
@Component
public class ChatMessageMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageMapper.class);
    
    /**
     * 领域模型转DTO（用于发送给前端）
     */
    public ChatMessageDTO toDTO(ChatMessage message) {
        if (message == null) {
            logger.warn("尝试转换null的ChatMessage");
            return null;
        }
        
        String type = message.getType();
        if (type == null) {
            logger.error("ChatMessage的type为null，messageId: {}", message.getMessageId());
            throw new IllegalArgumentException("ChatMessage的type不能为null");
        }
        
        return switch (type) {
            case "text" -> toTextMessageDTO(message);
            case "audio" -> toAudioMessageDTO(message);
            case "system" -> toSystemMessageDTO(message);
            case "error" -> toErrorMessageDTO(message);
            case "thinking" -> toThinkingMessageDTO(message);
            default -> {
                logger.warn("未知消息类型: {}, 使用SystemMessageDTO", type);
                yield toSystemMessageDTO(message);
            }
        };
    }
    
    /**
     * 转换为文本消息DTO
     */
    private TextMessageDTO toTextMessageDTO(ChatMessage message) {
        TextMessageDTO dto = new TextMessageDTO();
        copyBaseFields(message, dto);
        
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setStreaming(message.isStreaming());
        dto.setTtsMode(message.getTtsMode());
        dto.setSentenceOrder(message.getSentenceOrder());
        dto.setSentenceId(message.getSentenceId());
        dto.setDone(message.isStreamComplete());  // 使用streamComplete作为done
        
        logger.debug("转换文本消息: role={}, contentLength={}, streaming={}", 
                    dto.getRole(), 
                    dto.getContent() != null ? dto.getContent().length() : 0,
                    dto.getStreaming());
        
        return dto;
    }
    
    /**
     * 转换为音频消息DTO
     */
    private AudioMessageDTO toAudioMessageDTO(ChatMessage message) {
        AudioMessageDTO dto = new AudioMessageDTO();
        copyBaseFields(message, dto);
        
        dto.setContent(message.getContent());
        dto.setTtsMode(message.getTtsMode());
        dto.setSentenceOrder(message.getSentenceOrder());
        dto.setSentenceId(message.getSentenceId());
        
        // 转换音频数据为Base64
        byte[] audioBytes = message.getAudioData();
        if (audioBytes != null && audioBytes.length > 0) {
            try {
                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                dto.setAudioData(base64Audio);
                logger.debug("转换音频消息: audioSize={}bytes, base64Length={}", 
                            audioBytes.length, base64Audio.length());
            } catch (Exception e) {
                logger.error("Base64编码音频数据失败", e);
            }
        } else {
            logger.warn("音频消息没有音频数据: sentenceId={}", dto.getSentenceId());
        }
        
        // 设置音频格式（从audio对象获取，如果有的话）
        if (message.getAudioObject() != null && message.getAudioObject().getAudioFormat() != null) {
            dto.setAudioFormat(message.getAudioObject().getAudioFormat());
        }
        
        return dto;
    }
    
    /**
     * 转换为系统消息DTO
     */
    private SystemMessageDTO toSystemMessageDTO(ChatMessage message) {
        SystemMessageDTO dto = new SystemMessageDTO();
        copyBaseFields(message, dto);
        
        dto.setContent(message.getContent());
        
        // SubType从metadata中获取（如果有的话）
        if (message.getMetadata() != null) {
            Object subType = message.getMetadata().get("subType");
            if (subType != null) {
                dto.setSubType(subType.toString());
            }
            dto.setData(message.getMetadata());
        }
        
        logger.debug("转换系统消息: subType={}, content={}", 
                    dto.getSubType(), dto.getContent());
        
        return dto;
    }
    
    /**
     * 转换为错误消息DTO
     */
    private ErrorMessageDTO toErrorMessageDTO(ChatMessage message) {
        ErrorMessageDTO dto = new ErrorMessageDTO();
        copyBaseFields(message, dto);
        
        dto.setMessage(message.getContent());
        
        // ErrorCode和details从metadata中获取（如果有的话）
        if (message.getMetadata() != null) {
            Object errorCode = message.getMetadata().get("errorCode");
            if (errorCode != null) {
                dto.setErrorCode(errorCode.toString());
            }
            Object details = message.getMetadata().get("details");
            if (details != null) {
                dto.setDetails(details.toString());
            }
        }
        
        logger.debug("转换错误消息: errorCode={}, message={}", 
                    dto.getErrorCode(), dto.getMessage());
        
        return dto;
    }
    
    /**
     * 转换为思考消息DTO
     */
    private ThinkingMessageDTO toThinkingMessageDTO(ChatMessage message) {
        ThinkingMessageDTO dto = new ThinkingMessageDTO();
        copyBaseFields(message, dto);
        
        // 思考内容使用thinkingContent或content
        String content = message.getThinkingContent();
        if (content == null) {
            content = message.getContent();
        }
        dto.setContent(content);
        
        // Stage从metadata获取
        if (message.getMetadata() != null) {
            Object stage = message.getMetadata().get("stage");
            if (stage != null) {
                dto.setStage(stage.toString());
            }
        }
        
        dto.setStreaming(message.isStreaming());
        dto.setDone(message.isStreamComplete());
        
        logger.debug("转换思考消息: stage={}, streaming={}, done={}", 
                    dto.getStage(), dto.getStreaming(), dto.getDone());
        
        return dto;
    }
    
    /**
     * 复制基础字段
     */
    private void copyBaseFields(ChatMessage message, ChatMessageDTO dto) {
        dto.setSessionId(message.getSessionId());
        dto.setMessageId(message.getMessageId());
        dto.setTimestamp(message.getTimestamp());
        dto.setChannelType(message.getChannelType());
    }
    
    /**
     * DTO转领域模型（用于接收前端消息）
     */
    public ChatMessage fromDTO(ChatMessageDTO dto) {
        if (dto == null) {
            logger.warn("尝试转换null的ChatMessageDTO");
            return null;
        }
        
        ChatMessage message = new ChatMessage();
        message.setType(dto.getType());
        message.setSessionId(dto.getSessionId());
        message.setMessageId(dto.getMessageId());
        message.setTimestamp(dto.getTimestamp());
        message.setChannelType(dto.getChannelType());
        
        // 根据具体DTO类型设置特定字段
        if (dto instanceof TextMessageDTO textDTO) {
            message.setRole(textDTO.getRole());
            message.setContent(textDTO.getContent());
            if (textDTO.getStreaming() != null) {
                message.setStreaming(textDTO.getStreaming());
            }
            if (textDTO.getDone() != null) {
                message.setStreamComplete(textDTO.getDone());
            }
            message.setTtsMode(textDTO.getTtsMode());
            message.setSentenceOrder(textDTO.getSentenceOrder());
            message.setSentenceId(textDTO.getSentenceId());
            
            logger.debug("从DTO转换文本消息: role={}, contentLength={}", 
                        textDTO.getRole(), 
                        textDTO.getContent() != null ? textDTO.getContent().length() : 0);
            
        } else if (dto instanceof AudioMessageDTO audioDTO) {
            message.setContent(audioDTO.getContent());
            message.setTtsMode(audioDTO.getTtsMode());
            message.setSentenceOrder(audioDTO.getSentenceOrder());
            message.setSentenceId(audioDTO.getSentenceId());
            
            // 解码Base64音频数据
            if (audioDTO.getAudioData() != null && !audioDTO.getAudioData().isEmpty()) {
                try {
                    byte[] audioBytes = Base64.getDecoder().decode(audioDTO.getAudioData());
                    message.setAudioData(audioBytes);
                    
                    // 设置音频格式
                    if (audioDTO.getAudioFormat() != null && message.getAudioObject() != null) {
                        message.getAudioObject().setAudioFormat(audioDTO.getAudioFormat());
                    }
                    
                    logger.debug("从DTO转换音频消息: audioSize={}bytes", audioBytes.length);
                } catch (IllegalArgumentException e) {
                    logger.error("Base64解码音频数据失败", e);
                }
            }
            
        } else if (dto instanceof SystemMessageDTO systemDTO) {
            message.setContent(systemDTO.getContent());
            
            // SubType和data存入metadata
            if (systemDTO.getSubType() != null || systemDTO.getData() != null) {
                Map<String, Object> metadata = systemDTO.getData();
                if (metadata == null) {
                    metadata = new java.util.HashMap<>();
                }
                if (systemDTO.getSubType() != null) {
                    metadata.put("subType", systemDTO.getSubType());
                }
                message.setMetadata(metadata);
            }
            
            logger.debug("从DTO转换系统消息: subType={}", systemDTO.getSubType());
            
        } else if (dto instanceof ErrorMessageDTO errorDTO) {
            message.setContent(errorDTO.getMessage());
            
            // ErrorCode和details存入metadata
            if (errorDTO.getErrorCode() != null || errorDTO.getDetails() != null) {
                Map<String, Object> metadata = new java.util.HashMap<>();
                if (errorDTO.getErrorCode() != null) {
                    metadata.put("errorCode", errorDTO.getErrorCode());
                }
                if (errorDTO.getDetails() != null) {
                    metadata.put("details", errorDTO.getDetails());
                }
                message.setMetadata(metadata);
            }
            
            logger.debug("从DTO转换错误消息: errorCode={}", errorDTO.getErrorCode());
            
        } else if (dto instanceof ThinkingMessageDTO thinkingDTO) {
            message.setThinkingContent(thinkingDTO.getContent());
            if (thinkingDTO.getStreaming() != null) {
                message.setStreaming(thinkingDTO.getStreaming());
            }
            if (thinkingDTO.getDone() != null) {
                message.setStreamComplete(thinkingDTO.getDone());
            }
            
            // Stage存入metadata
            if (thinkingDTO.getStage() != null) {
                Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("stage", thinkingDTO.getStage());
                message.setMetadata(metadata);
            }
            
            logger.debug("从DTO转换思考消息: stage={}", thinkingDTO.getStage());
        }
        
        return message;
    }
}

