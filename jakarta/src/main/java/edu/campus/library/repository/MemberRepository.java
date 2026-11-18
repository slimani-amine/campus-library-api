package edu.campus.library.repository;

import edu.campus.library.domain.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    List<Member> findAll();

    Optional<Member> find(long id);

    Member save(Member member);
}
