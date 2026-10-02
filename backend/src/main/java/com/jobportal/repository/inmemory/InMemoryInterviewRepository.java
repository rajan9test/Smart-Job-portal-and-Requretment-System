package com.jobportal.repository.inmemory;

import com.jobportal.model.Interview;
import com.jobportal.repository.InterviewRepository;

import java.util.List;

public class InMemoryInterviewRepository extends InMemoryRepository<Interview> implements InterviewRepository {

    @Override
    public List<Interview> findByInterviewerEmail(String interviewerEmail) {
        String normalized = interviewerEmail.trim().toLowerCase();
        return findWhere(i -> i.getInterviewerEmail().equals(normalized));
    }

    @Override
    public List<Interview> findByCandidateId(Long candidateId) {
        return findWhere(i -> i.getCandidateId().equals(candidateId));
    }
}
