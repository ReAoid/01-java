package com.chatbot.controller;

import com.chatbot.model.domain.Persona;
import com.chatbot.service.knowledge.PersonaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人设管理控制器
 * 提供人设的CRUD操作接口
 */
@RestController
@RequestMapping("/api/personas")
public class PersonaController {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonaController.class);
    
    private final PersonaService personaService;
    
    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }
    
    /**
     * 获取所有人设列表
     */
    @GetMapping
    public List<Persona> getAllPersonas() {
        logger.debug("接收到获取所有人设列表请求");
        long startTime = System.currentTimeMillis();
        
        List<Persona> personas = personaService.getAllPersonas();
        
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("获取所有人设列表完成，共{}个人设，响应时间: {}ms", personas.size(), responseTime);
        return personas;
    }
    
    /**
     * 获取活跃人设列表
     */
    @GetMapping("/active")
    public List<Persona> getActivePersonas() {
        logger.debug("接收到获取活跃人设列表请求");
        long startTime = System.currentTimeMillis();
        
        List<Persona> activePersonas = personaService.getActivePersonas();
        
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("获取活跃人设列表完成，共{}个活跃人设，响应时间: {}ms", activePersonas.size(), responseTime);
        return activePersonas;
    }
    
    /**
     * 根据ID获取人设
     */
    @GetMapping("/{personaId}")
    public ResponseEntity<Persona> getPersona(@PathVariable String personaId) {
        logger.debug("接收到获取人设请求，personaId: {}", personaId);
        long startTime = System.currentTimeMillis();
        
        Persona persona = personaService.getPersona(personaId);
        long responseTime = System.currentTimeMillis() - startTime;
        
        if (persona != null) {
            logger.info("获取人设成功，personaId: {}, 响应时间: {}ms", personaId, responseTime);
            return ResponseEntity.ok(persona);
        } else {
            logger.warn("人设不存在，personaId: {}, 响应时间: {}ms", personaId, responseTime);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建新人设
     */
    @PostMapping
    public ResponseEntity<String> createPersona(@RequestBody Persona persona) {
        logger.debug("接收到创建人设请求，personaId: {}", persona != null ? persona.getPersonaId() : "null");
        long startTime = System.currentTimeMillis();
        
        try {
            if (persona.getPersonaId() == null || persona.getPersonaId().isEmpty()) {
                logger.warn("创建人设失败：人设ID为空");
                return ResponseEntity.badRequest().body("人设ID不能为空");
            }
            
            if (personaService.personaExists(persona.getPersonaId())) {
                logger.warn("创建人设失败：人设ID已存在，personaId: {}", persona.getPersonaId());
                return ResponseEntity.badRequest().body("人设ID已存在");
            }
            
            personaService.addPersona(persona);
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("人设创建成功，personaId: {}, 响应时间: {}ms", persona.getPersonaId(), responseTime);
            return ResponseEntity.ok("人设创建成功");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("创建人设异常，personaId: {}, 响应时间: {}ms", 
                        persona != null ? persona.getPersonaId() : "null", responseTime, e);
            return ResponseEntity.internalServerError().body("创建人设失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新人设
     */
    @PutMapping("/{personaId}")
    public ResponseEntity<String> updatePersona(@PathVariable String personaId, 
                                               @RequestBody Persona persona) {
        logger.debug("接收到更新人设请求，personaId: {}", personaId);
        long startTime = System.currentTimeMillis();
        
        try {
            if (!personaService.personaExists(personaId)) {
                logger.warn("更新人设失败：人设不存在，personaId: {}", personaId);
                return ResponseEntity.notFound().build();
            }
            
            persona.setPersonaId(personaId);
            personaService.updatePersona(persona);
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("人设更新成功，personaId: {}, 响应时间: {}ms", personaId, responseTime);
            return ResponseEntity.ok("人设更新成功");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("更新人设异常，personaId: {}, 响应时间: {}ms", personaId, responseTime, e);
            return ResponseEntity.internalServerError().body("更新人设失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除人设
     */
    @DeleteMapping("/{personaId}")
    public ResponseEntity<String> deletePersona(@PathVariable String personaId) {
        logger.debug("接收到删除人设请求，personaId: {}", personaId);
        long startTime = System.currentTimeMillis();
        
        try {
            if (personaId.equals(personaService.getDefaultPersonaId())) {
                logger.warn("删除人设失败：不能删除默认人设，personaId: {}", personaId);
                return ResponseEntity.badRequest().body("不能删除默认人设");
            }
            
            if (!personaService.personaExists(personaId)) {
                logger.warn("删除人设失败：人设不存在，personaId: {}", personaId);
                return ResponseEntity.notFound().build();
            }
            
            personaService.deletePersona(personaId);
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("人设删除成功，personaId: {}, 响应时间: {}ms", personaId, responseTime);
            return ResponseEntity.ok("人设删除成功");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("删除人设异常，personaId: {}, 响应时间: {}ms", personaId, responseTime, e);
            return ResponseEntity.internalServerError().body("删除人设失败: " + e.getMessage());
        }
    }
}
