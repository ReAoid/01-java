package com.chatbot.service.processor;

import com.chatbot.config.AppConfig;
import com.chatbot.config.properties.AIProperties;
import com.chatbot.model.domain.ChatMessage;
import com.chatbot.model.domain.ChatSession;
import com.chatbot.model.dto.llm.Message;
import com.chatbot.service.session.ChatHistoryService;
import com.chatbot.service.knowledge.KnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天上下文构建器
 * 负责构建对话上下文，包括系统提示词、历史记录、世界书设定等
 */
@Service
public class ChatContextBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatContextBuilder.class);
    
    private final KnowledgeService knowledgeService;
    private final ChatHistoryService chatHistoryService;
    private final ChatMessageProcessor messageProcessor;
    private final AIProperties aiConfig;
    
    // Token 配置
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final int DEFAULT_TOKENS_PER_CHAR = 4;  // 中文字符估算
    
    public ChatContextBuilder(KnowledgeService knowledgeService,
                             ChatHistoryService chatHistoryService,
                             ChatMessageProcessor messageProcessor,
                             AppConfig appConfig) {
        this.knowledgeService = knowledgeService;
        this.chatHistoryService = chatHistoryService;
        this.messageProcessor = messageProcessor;
        this.aiConfig = appConfig.getAi();
        
        logger.info("ChatContextBuilder 初始化完成，使用 KnowledgeService 统一知识管理");
    }
    
    /**
     * 获取系统提示词和人设提示词
     * 使用 KnowledgeService 统一获取人设信息
     */
    public List<ChatMessage> getSystemPrompts(ChatSession session) {
        List<ChatMessage> systemPrompts = new ArrayList<>();
        
        // 检查人设系统是否启用
        if (aiConfig.getSystemPrompt().isEnablePersona()) {
            // 使用 KnowledgeService 获取知识上下文（包含人设提示词）
            KnowledgeService.KnowledgeContext context = knowledgeService.retrieveRelevantContext(
                session.getSessionId(), "");
            
            // 如果有人设提示词，则使用人设提示词
            if (context.hasPersonaPrompt()) {
                ChatMessage personaMessage = createSystemMessage(
                    session.getSessionId(), context.getPersonaPrompt());
                systemPrompts.add(personaMessage);
                logger.debug("使用人设提示词，内容长度: {}", context.getPersonaPrompt().length());
                return systemPrompts;
            }
            
            // 人设提示词为空，使用系统提示词作为备用
            logger.warn("人设提示词为空，使用系统提示词作为备用");
        } else {
            logger.debug("人设系统已禁用，使用系统提示词");
        }
        
        // 添加系统提示词（备用方案）
        String baseSystemPrompt = aiConfig.getSystemPrompt().getBase();
        if (baseSystemPrompt == null || baseSystemPrompt.trim().isEmpty()) {
            baseSystemPrompt = aiConfig.getSystemPrompt().getFallback();
        }
        
        ChatMessage systemMessage = createSystemMessage(session.getSessionId(), baseSystemPrompt);
        systemPrompts.add(systemMessage);
        logger.debug("使用系统提示词作为备用，内容长度: {}", baseSystemPrompt.length());
        
        return systemPrompts;
    }
    
    /**
     * 获取历史对话记录（去掉系统提示词部分，只保留AI和用户对话历史）
     */
    public List<ChatMessage> getDialogueHistory(ChatSession session) {
        String sessionId = session.getSessionId();
        List<ChatMessage> dialogueMessages = new ArrayList<>();
        
        // 首先检查内存中是否已有对话历史
        List<ChatMessage> currentHistory = new ArrayList<>(session.getMessageHistory());
        List<ChatMessage> existingDialogue = currentHistory.stream()
            .filter(msg -> "user".equals(msg.getRole()) || "assistant".equals(msg.getRole()))
            .toList();
            
        if (!existingDialogue.isEmpty()) {
            logger.debug("从会话内存中获取对话历史，sessionId: {}, 消息数: {}", 
                       sessionId, existingDialogue.size());
            dialogueMessages.addAll(existingDialogue);
        } else {
            // 从文件加载历史记录
            List<ChatMessage> historyMessages = chatHistoryService.loadSessionHistory(sessionId);
            
            if (historyMessages != null && !historyMessages.isEmpty()) {
                // 过滤掉系统提示词部分，只保留AI和用户的对话历史
                List<ChatMessage> filteredDialogue = historyMessages.stream()
                    .filter(msg -> "user".equals(msg.getRole()) || "assistant".equals(msg.getRole()))
                    .toList();
                    
                logger.info("从文件加载对话历史，sessionId: {}, 原始消息数: {}, 过滤后对话消息数: {}", 
                           sessionId, historyMessages.size(), filteredDialogue.size());
                
                dialogueMessages.addAll(filteredDialogue);
            } else {
                logger.debug("没有找到历史记录文件或文件为空，sessionId: {}", sessionId);
            }
        }
        
        return dialogueMessages;
    }
    
    /**
     * 获取世界书设定和记忆内容
     * 使用 KnowledgeService 统一获取长期知识和短期记忆
     */
    public ChatMessage getWorldBookSetting(ChatSession session, String userInput) {
        try {
            // 使用 KnowledgeService 获取知识上下文
            KnowledgeService.KnowledgeContext context = knowledgeService.retrieveRelevantContext(
                session.getSessionId(), userInput);
            
            StringBuilder knowledgeContent = new StringBuilder();
            
            // 添加短期记忆
            if (context.hasShortTermMemory()) {
                knowledgeContent.append("【近期记忆】\n");
                knowledgeContent.append(context.getShortTermMemory());
            }
            
            // 添加长期知识（世界书）
            if (context.hasLongTermKnowledge()) {
                if (knowledgeContent.length() > 0) {
                    knowledgeContent.append("\n\n");
                }
                knowledgeContent.append("【相关知识】\n");
                knowledgeContent.append(context.getLongTermKnowledge());
            }
            
            if (knowledgeContent.length() > 0) {
                // 创建知识消息
                ChatMessage knowledgeMessage = createSystemMessage(
                    session.getSessionId(),
                    "为了回答用户的问题，你需要知道：\n" + knowledgeContent.toString()
                );
                
                logger.debug("创建知识上下文消息，内容长度: {}, 包含短期记忆: {}, 长期知识: {}", 
                           knowledgeContent.length(), context.hasShortTermMemory(), context.hasLongTermKnowledge());
                return knowledgeMessage;
            } else {
                logger.debug("没有找到相关的知识上下文");
                return null;
            }
        } catch (Exception e) {
            logger.error("获取知识上下文时发生错误", e);
            return null;
        }
    }
    
    /**
     * 构建完整的消息列表（带 token 限制和智能删除）
     */
    public List<Message> buildMessagesListWithTokenLimit(
            List<ChatMessage> systemPrompts,
            List<ChatMessage> dialogueHistory, 
            ChatMessage worldBookSetting,
            ChatMessage webSearchMessage,
            ChatMessage userMessage) {
        
        List<Message> messages = new ArrayList<>();
        
        final int MAX_TOKENS = DEFAULT_MAX_TOKENS;
        final int ESTIMATED_TOKENS_PER_CHAR = DEFAULT_TOKENS_PER_CHAR;
        
        int currentTokens = 0;
        
        // 1. 首先添加系统提示词（这些不能删除）
        for (ChatMessage systemMsg : systemPrompts) {
            if (systemMsg.getContent() != null && !systemMsg.getContent().trim().isEmpty()) {
                String role = messageProcessor.mapSenderToRole(systemMsg.getRole());
                messages.add(new Message(role, systemMsg.getContent()));
                currentTokens += estimateTokens(systemMsg.getContent(), ESTIMATED_TOKENS_PER_CHAR);
                logger.debug("添加系统消息: role={}, tokens={}", 
                           role, estimateTokens(systemMsg.getContent(), ESTIMATED_TOKENS_PER_CHAR));
            }
        }
        
        // 2. 添加联网搜索结果（如果有的话）
        if (webSearchMessage != null && webSearchMessage.getContent() != null 
            && !webSearchMessage.getContent().trim().isEmpty()) {
            String role = messageProcessor.mapSenderToRole(webSearchMessage.getRole());
            int webSearchTokens = estimateTokens(webSearchMessage.getContent(), ESTIMATED_TOKENS_PER_CHAR);
            
            if (currentTokens + webSearchTokens <= MAX_TOKENS) {
                messages.add(new Message(role, webSearchMessage.getContent()));
                currentTokens += webSearchTokens;
                logger.debug("添加联网搜索结果: tokens={}", webSearchTokens);
            } else {
                logger.warn("联网搜索结果超过 token 限制，跳过添加");
            }
        }
        
        // 3. 添加世界书设定（如果有的话）
        if (worldBookSetting != null && worldBookSetting.getContent() != null 
            && !worldBookSetting.getContent().trim().isEmpty()) {
            String role = messageProcessor.mapSenderToRole(worldBookSetting.getRole());
            int worldBookTokens = estimateTokens(worldBookSetting.getContent(), ESTIMATED_TOKENS_PER_CHAR);
            
            if (currentTokens + worldBookTokens <= MAX_TOKENS) {
                messages.add(new Message(role, worldBookSetting.getContent()));
                currentTokens += worldBookTokens;
                logger.debug("添加世界书设定: tokens={}", worldBookTokens);
            } else {
                logger.warn("世界书设定超过 token 限制，跳过添加");
            }
        }
        
        // 4. 添加当前用户消息（这个必须包含）
        if (userMessage != null && userMessage.getContent() != null 
            && !userMessage.getContent().trim().isEmpty()) {
            String role = messageProcessor.mapSenderToRole(userMessage.getRole());
            int userTokens = estimateTokens(userMessage.getContent(), ESTIMATED_TOKENS_PER_CHAR);
            messages.add(new Message(role, userMessage.getContent()));
            currentTokens += userTokens;
            logger.debug("添加用户消息: tokens={}", userTokens);
        }
        
        // 5. 智能添加对话历史（从最新的开始，向前添加，直到达到 token 限制）
        List<ChatMessage> filteredHistory = filterDialogueHistoryByTokens(
            dialogueHistory, MAX_TOKENS - currentTokens, ESTIMATED_TOKENS_PER_CHAR);
        
        // 将过滤后的历史消息插入到系统消息之后、用户消息之前
        int insertIndex = systemPrompts.size();
        if (webSearchMessage != null) {
            insertIndex++;
        }
        if (worldBookSetting != null) {
            insertIndex++;
        }
        
        for (ChatMessage historyMsg : filteredHistory) {
            if (historyMsg.getContent() != null && !historyMsg.getContent().trim().isEmpty()) {
                String role = messageProcessor.mapSenderToRole(historyMsg.getRole());
                messages.add(insertIndex++, new Message(role, historyMsg.getContent()));
                logger.debug("添加历史对话: role={}, contentLength={}", 
                           role, historyMsg.getContent().length());
            }
        }
        
        // 计算最终的 token 数量
        int finalTokens = messages.stream()
            .mapToInt(msg -> estimateTokens(msg.getContent(), ESTIMATED_TOKENS_PER_CHAR))
            .sum();
            
        logger.info("消息列表构建完成 - 总消息数: {}, 估算 tokens: {}/{}, 系统消息: {}, 历史消息: {}, 联网搜索: {}, 世界书: {}, 用户消息: 1", 
                   messages.size(), finalTokens, MAX_TOKENS, systemPrompts.size(), 
                   filteredHistory.size(), webSearchMessage != null ? 1 : 0, worldBookSetting != null ? 1 : 0);
        
        return messages;
    }
    
    /**
     * 估算文本的 token 数量
     */
    private int estimateTokens(String text, int tokensPerChar) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / tokensPerChar;
    }
    
    /**
     * 根据 token 限制过滤对话历史（从最远的开始删除）
     */
    private List<ChatMessage> filterDialogueHistoryByTokens(
            List<ChatMessage> dialogueHistory, int maxTokens, int tokensPerChar) {
        if (dialogueHistory == null || dialogueHistory.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<ChatMessage> result = new ArrayList<>();
        int currentTokens = 0;
        
        // 从最新的消息开始向前检查（倒序遍历）
        for (int i = dialogueHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = dialogueHistory.get(i);
            if (msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                int msgTokens = estimateTokens(msg.getContent(), tokensPerChar);
                
                if (currentTokens + msgTokens <= maxTokens) {
                    result.add(0, msg); // 插入到列表开头以保持原有顺序
                    currentTokens += msgTokens;
                } else {
                    // 超过限制，停止添加更早的消息
                    logger.debug("历史消息超过 token 限制，丢弃 {} 条更早的消息", i + 1);
                    break;
                }
            }
        }
        
        logger.debug("过滤对话历史完成，保留 {}/{} 条消息，使用 tokens: {}/{}", 
                   result.size(), dialogueHistory.size(), currentTokens, maxTokens);
        
        return result;
    }
    
    /**
     * 创建系统消息的辅助方法
     */
    private ChatMessage createSystemMessage(String sessionId, String content) {
        ChatMessage message = new ChatMessage();
        message.setRole("system");
        message.setContent(content);
        message.setSessionId(sessionId);
        message.setType("text");
        return message;
    }
}

