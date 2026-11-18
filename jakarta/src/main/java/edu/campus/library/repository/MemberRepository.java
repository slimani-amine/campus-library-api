package edu.campus.library.repository;

import edu.campus.library.domain.Member;

import java.util.List;
import java.util.Optional;

/** Where members are stored. */
public interface MemberRepository {

    List<Member> findAll();

    Optional<Member> find(long id);

    /** Inserts the member if its id is null (a new id is assigned), otherwise replaces it. */
    Member save(Member member);
}
