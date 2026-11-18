package edu.campus.library.repository;

import edu.campus.library.domain.Member;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Keeps members in memory. One instance for the whole application (@ApplicationScoped). */
@ApplicationScoped
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<Long, Member> members = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(members.values());
    }

    @Override
    public Optional<Member> find(long id) {
        return Optional.ofNullable(members.get(id));
    }

    @Override
    public Member save(Member member) {
        Member stored = member.id() == null
                ? new Member(nextId.getAndIncrement(), member.name(), member.email())
                : member;
        members.put(stored.id(), stored);
        return stored;
    }
}
