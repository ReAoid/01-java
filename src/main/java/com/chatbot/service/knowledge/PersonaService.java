package com.chatbot.service.knowledge;

import com.chatbot.config.AppConfig;
import com.chatbot.model.domain.Persona;
import com.chatbot.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final AppConfig appConfig;
    private static final String DEFAULT_PERSONA_ID = "default";
    private static final String PERSONAS_FILE = "personas.json";
    private boolean isLoadedFromExternalFile = false;  // 标记是否从外部文件成功加载
    
    public PersonaService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.personas = new ConcurrentHashMap<>();
        initializePersonas();
    }
    
    /**
     * 初始化人设（从外部文件读取）
     */
    private void initializePersonas() {
        try {
            // 先尝试从外部文件读取
            loadPersonasFromFile();
            isLoadedFromExternalFile = true;  // 标记成功从外部文件加载
            logger.info("成功从外部文件加载人设配置");
        } catch (Exception e) {
            logger.warn("从外部文件加载人设失败，使用内置默认人设: {}", e.getMessage());
            // 如果文件读取失败，使用内置默认人设
            initializeBuiltinPersonas();
            isLoadedFromExternalFile = false;  // 标记使用内置人设
        }
        
        logger.info("初始化了 {} 个人设", personas.size());
    }
    
    /**
     * 从外部JSON文件加载人设
     */
    private void loadPersonasFromFile() throws IOException {
        String personasPath = appConfig.getResource().getPersonasPath();
        Path personasFilePath = Paths.get(personasPath, PERSONAS_FILE);
        
        if (!Files.exists(personasFilePath)) {
            throw new IOException("人设文件不存在: " + personasFilePath);
        }
        
        String jsonContent = Files.readString(personasFilePath);
        List<Persona> personaList = JsonUtil.fromJsonToList(jsonContent, Persona.class);
        
        if (personaList != null && !personaList.isEmpty()) {
            for (Persona persona : personaList) {
                if (persona.getPersonaId() != null) {
                    personas.put(persona.getPersonaId(), persona);
                    logger.debug("加载人设: {} - {}", persona.getPersonaId(), persona.getName());
                }
            }
            logger.info("从文件 {} 成功加载了 {} 个人设", personasFilePath, personaList.size());
        } else {
            throw new IOException("人设文件内容为空或格式错误");
        }
    }
    
    /**
     * 初始化内置默认人设（备用方案）
     */
    private void initializeBuiltinPersonas() {
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
        
        logger.info("使用内置默认人设，共 {} 个", personas.size());
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
    
    /**
     * 保存人设到外部JSON文件（格式化）
     */
    public boolean savePersonasToFile() {
        try {
            String personasPath = appConfig.getResource().getPersonasPath();
            Path personasFilePath = Paths.get(personasPath, PERSONAS_FILE);
            
            List<Persona> personaList = new ArrayList<>(personas.values());
            String jsonContent = JsonUtil.toPrettyJson(personaList);
            
            Files.createDirectories(personasFilePath.getParent());
            Files.writeString(personasFilePath, jsonContent);
            
            logger.info("人设配置已保存到文件: {}, 共 {} 个人设", personasFilePath, personaList.size());
            return true;
            
        } catch (Exception e) {
            logger.error("保存人设配置到文件失败", e);
            return false;
        }
    }
    
    /**
     * 检查人设是否从外部文件成功加载
     */
    public boolean isLoadedFromExternalFile() {
        return isLoadedFromExternalFile;
    }
    
    /**
     * 重新加载人设配置
     */
    public boolean reloadPersonas() {
        try {
            personas.clear();
            isLoadedFromExternalFile = false;  // 重置标记
            initializePersonas();
            logger.info("人设配置重新加载成功，共 {} 个人设", personas.size());
            return true;
        } catch (Exception e) {
            logger.error("重新加载人设配置失败", e);
            return false;
        }
    }
}
