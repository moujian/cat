package com.dianping.cat.report.page.problem.service;

import java.util.Date;

import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.BasePayload;
import com.dianping.cat.consumer.problem.ProblemAnalyzer;
import com.dianping.cat.consumer.problem.model.entity.Entry;
import com.dianping.cat.consumer.problem.model.entity.JavaThread;
import com.dianping.cat.consumer.problem.model.entity.Machine;
import com.dianping.cat.consumer.problem.model.entity.ProblemReport;
import com.dianping.cat.consumer.problem.model.entity.Segment;
import com.dianping.cat.consumer.problem.model.transform.DefaultSaxParser;
import com.dianping.cat.helper.TimeHelper;
import com.dianping.cat.report.service.LocalModelService;
import com.dianping.cat.service.ModelPeriod;
import com.dianping.cat.service.ModelRequest;
import com.dianping.cat.storage.report.ReportBucket;
import com.dianping.cat.storage.report.ReportBucketManager;

public class LocalProblemService extends LocalModelService<ProblemReport> {
	
	public static final String ID = ProblemAnalyzer.ID;

	@Inject
	private ReportBucketManager m_bucketManager;

	public LocalProblemService() {
		super(ProblemAnalyzer.ID);
	}

	private String filterReport(BasePayload payload, ProblemReport report) {
	   String ipAddress = payload.getIpAddress();;
		String type = payload.getType();
		String queryType = payload.getQueryType();
		String name = payload.getName();
		ProblemReportFilter filter = new ProblemReportFilter(ipAddress , type, queryType,
		      name);

		return filter.buildXml(report);
   }

	@Override
	public String getReport(ModelRequest request, ModelPeriod period, String domain,BasePayload payload) throws Exception {
		ProblemReport report = super.getReport( period, domain);

		if ((report == null || report.getIps().isEmpty()) && period.isLast()) {
			long startTime = request.getStartTime();
			report = getReportFromLocalDisk(startTime, domain);
		}
		return filterReport(payload, report);
	}

	private ProblemReport getReportFromLocalDisk(long timestamp, String domain) throws Exception {
		ReportBucket<String> bucket = null;

		try {
			bucket = m_bucketManager.getReportBucket(timestamp, ProblemAnalyzer.ID);
			String xml = bucket.findById(domain);
			ProblemReport report = null;

			if (xml != null) {
				report = DefaultSaxParser.parse(xml);
			} else {
				report = new ProblemReport(domain);
				report.setStartTime(new Date(timestamp));
				report.setEndTime(new Date(timestamp + TimeHelper.ONE_HOUR - 1));
				report.getDomainNames().addAll(bucket.getIds());
			}
			return report;

		} finally {
			if (bucket != null) {
				m_bucketManager.closeBucket(bucket);
			}
		}
	}
	

	public static class ProblemReportFilter extends com.dianping.cat.consumer.problem.model.transform.DefaultXmlBuilder {
		private String m_ipAddress;

		// view is show the summary,detail show the thread info
		private String m_type;

		private String m_queryType;

		private String m_status;

		public ProblemReportFilter(String ipAddress, String type, String queryType, String name) {
			super(true, new StringBuilder(DEFAULT_SIZE));
			m_ipAddress = ipAddress;
			m_type = type;
			m_status = name;
			m_queryType = queryType;
		}

		@Override
		public void visitDuration(com.dianping.cat.consumer.problem.model.entity.Duration duration) {
			super.visitDuration(duration);
		}

		@Override
		public void visitEntry(Entry entry) {
			if (m_type == null) {
				super.visitEntry(entry);
			} else {
				if (m_status == null) {
					if (entry.getType().equals(m_type)) {
						super.visitEntry(entry);
					}
				} else {
					if (entry.getType().equals(m_type) && entry.getStatus().equals(m_status)) {
						super.visitEntry(entry);
					}
				}
			}
		}

		@Override
		public void visitMachine(Machine machine) {
			if (m_ipAddress == null) {
				super.visitMachine(machine);
			} else if (machine.getIp().equals(m_ipAddress)) {
				super.visitMachine(machine);
			}
		}

		@Override
		public void visitSegment(Segment segment) {
			super.visitSegment(segment);
		}

		@Override
		public void visitThread(JavaThread thread) {
			if ("detail".equals(m_queryType)) {
				super.visitThread(thread);
			}
		}
	}
}
