/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.benchmark;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.es.EsClient;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.source.index.SourceLineResultSetIterator;
import org.sonar.server.tester.ServerTester;

import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests of the Elasticsearch index sourcelines
 * <ul>
 *   <li>throughput of indexing of documents</li>
 *   <li>size of ES data directory</li>
 *   <li>time to request index</li>
 * </ul>
 */
public class SourceIndexBenchmarkTest {

  private static final Logger LOGGER = LoggerFactory.getLogger("benchmarkSourceIndexing");
  private static final long FILES = 1000L;
  private static final int LINES_PER_FILE = 3220;

  @Rule
  public ServerTester tester = new ServerTester();

  @Rule
  public Benchmark benchmark = new Benchmark();

  @Test
  public void benchmark() throws Exception {
    // index source lines
    benchmarkIndexing();

    // execute some queries
    benchmarkQueries();
  }

  private void benchmarkIndexing() {
    LOGGER.info("Indexing source lines");

    SourceIterator files = new SourceIterator(FILES, LINES_PER_FILE);
    ProgressTask progressTask = new ProgressTask(LOGGER, "files of " + LINES_PER_FILE + " lines", files.count());
    Timer timer = new Timer("SourceIndexer");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);

    long start = System.currentTimeMillis();
    tester.get(SourceLineIndexer.class).index(files);
    long end = System.currentTimeMillis();

    timer.cancel();
    long period = end - start;
    long nbLines = files.count.get() * LINES_PER_FILE;
    long throughputPerSecond = 1000L * nbLines / period;
    LOGGER.info(String.format("%d lines indexed in %d ms (%d docs/second)", nbLines, period, throughputPerSecond));
    benchmark.expectBetween("Throughput to index source lines", throughputPerSecond, 7500, 8000);

    // be sure that physical files do not evolve during estimation of size
    tester.get(EsClient.class).prepareOptimize(SourceLineIndexDefinition.INDEX).get();
    long dirSize = FileUtils.sizeOfDirectory(tester.getEsServerHolder().getHomeDir());
    LOGGER.info(String.format("ES dir: " + FileUtils.byteCountToDisplaySize(dirSize)));
    benchmark.expectBetween("ES dir size (b)", dirSize, 103L * FileUtils.ONE_MB, 109L * FileUtils.ONE_MB);
  }

  private void benchmarkQueries() {
    SourceLineIndex index = tester.get(SourceLineIndex.class);
    for (int i = 1; i <= 100; i++) {
      long start = System.currentTimeMillis();
      List<SourceLineDoc> result = index.getLines("FILE" + i, 20, 150);
      long end = System.currentTimeMillis();
      assertThat(result).hasSize(131);
      LOGGER.info("Request: {} docs in {} ms", result.size(), end - start);
    }
    // TODO assertions
  }

  private static class SourceIterator implements Iterator<SourceLineResultSetIterator.SourceFile> {
    private final long nbFiles;
    private final int nbLinesPerFile;
    private int currentProject = 0;
    private AtomicLong count = new AtomicLong(0L);

    SourceIterator(long nbFiles, int nbLinesPerFile) {
      this.nbFiles = nbFiles;
      this.nbLinesPerFile = nbLinesPerFile;
    }

    public AtomicLong count() {
      return count;
    }

    @Override
    public boolean hasNext() {
      return count.get() < nbFiles;
    }

    @Override
    public SourceLineResultSetIterator.SourceFile next() {
      String fileUuid = "FILE" + count.get();
      SourceLineResultSetIterator.SourceFile file = new SourceLineResultSetIterator.SourceFile(fileUuid, System.currentTimeMillis());
      for (int indexLine = 1; indexLine <= nbLinesPerFile; indexLine++) {
        SourceLineDoc line = new SourceLineDoc(Maps.<String, Object>newHashMap());
        line.setFileUuid(fileUuid);
        line.setLine(indexLine);
        line.setHighlighting(StringUtils.repeat("HIGHLIGHTING", 5));
        line.setItConditions(4);
        line.setItCoveredConditions(2);
        line.setItLineHits(2);
        line.setOverallConditions(8);
        line.setOverallCoveredConditions(2);
        line.setOverallLineHits(2);
        line.setUtConditions(8);
        line.setUtCoveredConditions(2);
        line.setUtLineHits(2);
        line.setProjectUuid("PROJECT" + currentProject);
        line.setScmAuthor("a_guy");
        line.setScmRevision("ABCDEFGHIJKL");
        line.setSource(StringUtils.repeat("SOURCE", 10));
        file.addLine(line);
      }
      count.incrementAndGet();
      if (count.get() % 500 == 0) {
        currentProject++;
      }
      return file;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
