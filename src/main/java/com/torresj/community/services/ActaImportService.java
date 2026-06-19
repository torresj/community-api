package com.torresj.community.services;

import com.torresj.community.dtos.ReportDto;
import com.torresj.community.exceptions.CommunityNotFoundException;
import com.torresj.community.exceptions.ReportItemNotFoundException;
import com.torresj.community.exceptions.ReportNotFoundException;

public interface ActaImportService {
    /**
     * Import an acta PDF into a community: store the PDF, auto-extract what it can, and create a
     * DRAFT meeting (with agenda items and voting results where extracted) for the admin to review.
     */
    ReportDto importPdf(long communityId, byte[] pdf, String filename)
            throws CommunityNotFoundException, ReportNotFoundException, ReportItemNotFoundException;
}
