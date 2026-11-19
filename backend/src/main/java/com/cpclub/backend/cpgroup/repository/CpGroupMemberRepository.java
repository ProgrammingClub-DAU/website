package com.cpclub.backend.cpgroup.repository;

import com.cpclub.backend.cpgroup.entity.CpGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CpGroupMemberRepository extends JpaRepository<CpGroupMember, Long> {
}
