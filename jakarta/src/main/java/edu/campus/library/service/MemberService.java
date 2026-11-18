package edu.campus.library.service;

import edu.campus.library.domain.Member;
import edu.campus.library.domain.NewMember;
import edu.campus.library.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
@Logged
public class MemberService {

    private MemberRepository members;

    protected MemberService() {
    }

    @Inject
    public MemberService(MemberRepository members) {
        this.members = members;
    }

    public List<Member> findAll() {
        return members.findAll();
    }

    public Member create(NewMember newMember) {
        return members.save(new Member(null, newMember.name(), newMember.email()));
    }
}
