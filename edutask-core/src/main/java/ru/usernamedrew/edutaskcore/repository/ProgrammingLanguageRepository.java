package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.ProgrammingLanguage;

import java.util.Optional;

public interface ProgrammingLanguageRepository extends JpaRepository<ProgrammingLanguage, Integer> {
    Optional<ProgrammingLanguage> findByCodeIgnoreCase(String code);
}
