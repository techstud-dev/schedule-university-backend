package com.techstud.schedule_university.auth.service.impl;

import com.techstud.schedule_university.auth.entity.Role;
import com.techstud.schedule_university.auth.repository.RoleRepository;
import com.techstud.schedule_university.auth.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис управления ролями пользователей
 *
 * <p>Обеспечивает создание и поиск ролей по требованию</p>
 */
@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository repository;

    public RoleServiceImpl(RoleRepository repository) {
        this.repository = repository;
    }

    /**
     * Находит или создает новую роль
     *
     * @param name Название роли
     * @return Существующая или новая роль
     */
    @Override
    @Transactional
    public Role resolveRole(String name) {
        return repository.findByName(name)
                .orElseGet(() -> createNewRole(name));
    }

    private Role createNewRole(String name) {
        Role role = new Role(name);
        return repository.save(role);
    }
}
