package ru.usernamedrew.edutaskcore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.usernamedrew.edutaskcore.entity.ProgrammingLanguage;

public interface ProgrammingLanguageRepository extends JpaRepository<ProgrammingLanguage, Integer> {
}
