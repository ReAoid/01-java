package com.chatbot.service;

import com.chatbot.model.Persona;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 人设管理服务
 * 负责管理AI机器人的人设配置
 */
@Service
public class PersonaService {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonaService.class);
    
    private final ConcurrentHashMap<String, Persona> personas;
    private static final String DEFAULT_PERSONA_ID = "default";
    
    public PersonaService() {
        this.personas = new ConcurrentHashMap<>();
        initializeDefaultPersonas();
    }
    
    /**
     * 初始化默认人设
     */
    private void initializeDefaultPersonas() {
        // 默认助手人设
        Persona defaultPersona = new Persona(DEFAULT_PERSONA_ID, "智能助手");
        defaultPersona.setDescription("一个友善、专业的AI助手");
        defaultPersona.setPersonality("友善、耐心、专业、乐于助人");
        defaultPersona.setKnowledgeScope("广泛的知识领域，包括科学、技术、文化、生活等");
        defaultPersona.setCommunicationStyle("清晰、简洁、友好的交流方式");
        defaultPersona.setSystemPrompt(
            "你是一个智能AI助手，具有以下特点：\n" +
            "- 友善耐心，乐于帮助用户解决问题\n" +
            "- 回答准确专业，逻辑清晰\n" +
            "- 语言表达简洁明了，易于理解\n" +
            "- 对不确定的信息会诚实说明\n" +
            "- 始终保持积极正面的态度"
        );
        personas.put(DEFAULT_PERSONA_ID, defaultPersona);
        
        // 专业顾问人设
        Persona advisorPersona = new Persona("advisor", "专业顾问");
        advisorPersona.setDescription("专业的咨询顾问，提供深度分析和建议");
        advisorPersona.setPersonality("严谨、分析性强、逻辑清晰");
        advisorPersona.setKnowledgeScope("商业分析、战略规划、问题解决");
        advisorPersona.setCommunicationStyle("结构化、分析性的表达方式");
        advisorPersona.setSystemPrompt(
            "你是一位专业的咨询顾问，具有以下特点：\n" +
            "- 善于分析复杂问题，提供结构化的解决方案\n" +
            "- 回答逻辑严密，条理清晰\n" +
            "- 会从多个角度分析问题\n" +
            "- 提供具体可行的建议和步骤\n" +
            "- 语言专业但不失亲和力"
        );
        personas.put("advisor", advisorPersona);
        
        // 创意助手人设
        Persona creativePersona = new Persona("creative", "创意助手");
        creativePersona.setDescription("富有创造力的助手，善于激发灵感");
        creativePersona.setPersonality("创新、活跃、想象力丰富");
        creativePersona.setKnowledgeScope("艺术、设计、创意思维、文学创作");
        creativePersona.setCommunicationStyle("生动活泼、富有想象力的表达");
        creativePersona.setSystemPrompt(
            "你是一个富有创造力的AI助手，具有以下特点：\n" +
            "- 思维活跃，善于提供创新的想法和解决方案\n" +
            "- 语言生动有趣，富有表现力\n" +
            "- 能够从不同角度启发用户的创意思维\n" +
            "- 鼓励用户尝试新的可能性\n" +
            "- 回答中经常使用比喻和生动的例子"
        );
        personas.put("creative", creativePersona);
        
        logger.info("初始化了 {} 个默认人设", personas.size());
    }
    
    /**
     * 获取人设
     */
    public Persona getPersona(String personaId) {
        return personas.get(personaId != null ? personaId : DEFAULT_PERSONA_ID);
    }
    
    /**
     * 获取人设的系统提示词
     */
    public String getPersonaPrompt(String personaId) {
        Persona persona = getPersona(personaId);
        return persona != null ? persona.getSystemPrompt() : getDefaultPersonaPrompt();
    }
    
    /**
     * 获取默认人设提示词
     */
    private String getDefaultPersonaPrompt() {
        Persona defaultPersona = personas.get(DEFAULT_PERSONA_ID);
        return defaultPersona != null ? defaultPersona.getSystemPrompt() : 
               "你是一个友善的AI助手，请帮助用户解决问题。";
    }
    
    /**
     * 添加新人设
     */
    public void addPersona(Persona persona) {
        if (persona != null && persona.getPersonaId() != null) {
            personas.put(persona.getPersonaId(), persona);
            logger.info("添加新人设: {} - {}", persona.getPersonaId(), persona.getName());
        }
    }
    
    /**
     * 更新人设
     */
    public void updatePersona(Persona persona) {
        if (persona != null && persona.getPersonaId() != null && 
            personas.containsKey(persona.getPersonaId())) {
            personas.put(persona.getPersonaId(), persona);
            logger.info("更新人设: {} - {}", persona.getPersonaId(), persona.getName());
        }
    }
    
    /**
     * 删除人设
     */
    public void deletePersona(String personaId) {
        if (personaId != null && !DEFAULT_PERSONA_ID.equals(personaId)) {
            Persona removed = personas.remove(personaId);
            if (removed != null) {
                logger.info("删除人设: {} - {}", personaId, removed.getName());
            }
        }
    }
    
    /**
     * 获取所有人设列表
     */
    public List<Persona> getAllPersonas() {
        return new ArrayList<>(personas.values());
    }
    
    /**
     * 获取活跃人设列表
     */
    public List<Persona> getActivePersonas() {
        return personas.values().stream()
                .filter(Persona::isActive)
                .toList();
    }
    
    /**
     * 检查人设是否存在
     */
    public boolean personaExists(String personaId) {
        return personas.containsKey(personaId);
    }
    
    /**
     * 获取默认人设ID
     */
    public String getDefaultPersonaId() {
        return DEFAULT_PERSONA_ID;
    }
}
