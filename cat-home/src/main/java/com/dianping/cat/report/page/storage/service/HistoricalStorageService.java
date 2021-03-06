package com.dianping.cat.report.page.storage.service;

import java.util.Date;

import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.consumer.storage.StorageAnalyzer;
import com.dianping.cat.consumer.storage.model.entity.StorageReport;
import com.dianping.cat.consumer.storage.model.transform.DefaultSaxParser;
import com.dianping.cat.helper.TimeHelper;
import com.dianping.cat.report.page.storage.task.StorageReportService;
import com.dianping.cat.report.service.BaseHistoricalModelService;
import com.dianping.cat.service.ModelRequest;
import com.dianping.cat.storage.report.ReportBucket;
import com.dianping.cat.storage.report.ReportBucketManager;

public class HistoricalStorageService extends BaseHistoricalModelService<StorageReport> {
	@Inject
	private ReportBucketManager m_bucketManager;

	@Inject
	private StorageReportService m_reportService;

	public HistoricalStorageService() {
		super(StorageAnalyzer.ID);
	}

	@Override
	protected StorageReport buildModel(ModelRequest request) throws Exception {
		String domain = request.getDomain();
		long date = request.getStartTime();
		StorageReport report;

		if (isLocalMode()) {
			report = getReportFromLocalDisk(date, domain);
		} else {
			report = getReportFromDatabase(date, domain);
		}

		return report;
	}

	private StorageReport getReportFromDatabase(long timestamp, String id) throws Exception {
		return m_reportService.queryReport(id, new Date(timestamp), new Date(timestamp + TimeHelper.ONE_HOUR));
	}

	private StorageReport getReportFromLocalDisk(long timestamp, String id) throws Exception {
		ReportBucket<String> bucket = null;
		try {
			bucket = m_bucketManager.getReportBucket(timestamp, StorageAnalyzer.ID);
			String xml = bucket.findById(id);

			return xml == null ? null : DefaultSaxParser.parse(xml);
		} finally {
			if (bucket != null) {
				m_bucketManager.closeBucket(bucket);
			}
		}
	}
}
