package com.jobportal.repository;

import com.jobportal.model.Interview;

import java.util.List;

public interface InterviewRepository extends Repository<Interview, Long> {

    List<Interview> findByInterviewerEmail(String interviewerEmail);

    List<Interview> findByCandidateId(Long candidateId);
}
