package com.techstud.schedule_university.auth.service.impl;

import com.techstud.schedule_university.auth.entity.PendingRegistration;
import com.techstud.schedule_university.auth.entity.User;
import com.techstud.schedule_university.auth.exception.UserExistsException;
import com.techstud.schedule_university.auth.repository.UserRepository;
import com.techstud.schedule_university.auth.service.RoleService;
import com.techstud.schedule_university.auth.service.UniversityService;
import com.techstud.schedule_university.auth.service.UserCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Сервис создания пользователей
 *
 * <p>Выполняет проверку уникальности данных и создание пользователя</p>
 */
@Service
@RequiredArgsConstructor
public class UserCreationServiceImpl implements UserCreationService {
    private final UserRepository repository;
    private final RoleService roleService;
    private final UniversityService universityService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Создает нового пользователя
     *
     * @param pendingRegistration Данные для регистрации
     * @return Созданный пользователь
     * @throws UserExistsException Если пользователь с такими данными уже существует
     */
    @Override
    @Transactional
    public User createPendingUser(PendingRegistration pendingRegistration) throws UserExistsException {
        validateUser(pendingRegistration);

        return User.builder()
                .username(pendingRegistration.getUsername())
                .fullName(pendingRegistration.getFullName())
                .password(passwordEncoder.encode(pendingRegistration.getPassword()))
                .email(pendingRegistration.getEmail())
                .phoneNumber(pendingRegistration.getPhoneNumber())
                .groupNumber(pendingRegistration.getGroupNumber())
                .university(universityService.resolveUniversity(pendingRegistration.getUniversity()))
                .roles(Set.of(roleService.resolveRole("USER")))
                .build();
    }

    /**
     * Валидирует пользователя
     *
     * @param pendingRegistration входящий класс пользователя
     * @throws UserExistsException если пользователь уже есть в бд бросает исключение
     */
    private void validateUser(PendingRegistration pendingRegistration) throws UserExistsException {
        if (repository.existsByUniqueFields(
                pendingRegistration.getUsername(), pendingRegistration.getEmail(), pendingRegistration.getPhoneNumber())) {
            throw new UserExistsException();
        }
    }
}
