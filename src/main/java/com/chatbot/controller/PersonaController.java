package com.chatbot.controller;

import com.chatbot.model.Persona;
import com.chatbot.service.PersonaService;
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
    
    private final PersonaService personaService;
    
    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }
    
    /**
     * 获取所有人设列表
     */
    @GetMapping
    public List<Persona> getAllPersonas() {
        return personaService.getAllPersonas();
    }
    
    /**
     * 获取活跃人设列表
     */
    @GetMapping("/active")
    public List<Persona> getActivePersonas() {
        return personaService.getActivePersonas();
    }
    
    /**
     * 根据ID获取人设
     */
    @GetMapping("/{personaId}")
    public ResponseEntity<Persona> getPersona(@PathVariable String personaId) {
        Persona persona = personaService.getPersona(personaId);
        if (persona != null) {
            return ResponseEntity.ok(persona);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建新人设
     */
    @PostMapping
    public ResponseEntity<String> createPersona(@RequestBody Persona persona) {
        try {
            if (persona.getPersonaId() == null || persona.getPersonaId().isEmpty()) {
                return ResponseEntity.badRequest().body("人设ID不能为空");
            }
            
            if (personaService.personaExists(persona.getPersonaId())) {
                return ResponseEntity.badRequest().body("人设ID已存在");
            }
            
            personaService.addPersona(persona);
            return ResponseEntity.ok("人设创建成功");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("创建人设失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新人设
     */
    @PutMapping("/{personaId}")
    public ResponseEntity<String> updatePersona(@PathVariable String personaId, 
                                               @RequestBody Persona persona) {
        try {
            if (!personaService.personaExists(personaId)) {
                return ResponseEntity.notFound().build();
            }
            
            persona.setPersonaId(personaId);
            personaService.updatePersona(persona);
            return ResponseEntity.ok("人设更新成功");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("更新人设失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除人设
     */
    @DeleteMapping("/{personaId}")
    public ResponseEntity<String> deletePersona(@PathVariable String personaId) {
        try {
            if (personaId.equals(personaService.getDefaultPersonaId())) {
                return ResponseEntity.badRequest().body("不能删除默认人设");
            }
            
            if (!personaService.personaExists(personaId)) {
                return ResponseEntity.notFound().build();
            }
            
            personaService.deletePersona(personaId);
            return ResponseEntity.ok("人设删除成功");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除人设失败: " + e.getMessage());
        }
    }
}
