/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.polaris.core.storage;

import java.util.Map;
import org.apache.polaris.core.entity.table.IcebergTableLikeEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class StorageUtilTest {

  @Test
  public void testEmptyString() {
    Assertions.assertThat(StorageUtil.getBucket("")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"s3", "s3a", "gcs", "abfs", "wasb", "file"})
  public void testAbsolutePaths(String scheme) {
    Assertions.assertThat(StorageUtil.getBucket(scheme + "://bucket/path/file.txt"))
        .isEqualTo("bucket");
    Assertions.assertThat(StorageUtil.getBucket(scheme + "://bucket:with:colon/path/file.txt"))
        .isEqualTo("bucket:with:colon");
    Assertions.assertThat(StorageUtil.getBucket(scheme + "://bucket_with_underscore/path/file.txt"))
        .isEqualTo("bucket_with_underscore");
    Assertions.assertThat(StorageUtil.getBucket(scheme + "://bucket_with_ユニコード/path/file.txt"))
        .isEqualTo("bucket_with_ユニコード");
  }

  @Test
  public void testRelativePaths() {
    Assertions.assertThat(StorageUtil.getBucket("bucket/path/file.txt")).isNull();
    Assertions.assertThat(StorageUtil.getBucket("path/file.txt")).isNull();
  }

  @Test
  public void testAbsolutePathWithoutScheme() {
    Assertions.assertThat(StorageUtil.getBucket("/bucket/path/file.txt")).isNull();
  }

  @Test
  public void testInvalidURI() {
    Assertions.assertThatThrownBy(
            () -> StorageUtil.getBucket("s3://bucket with space/path/file.txt"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testAuthorityWithPort() {
    Assertions.assertThat(StorageUtil.getBucket("s3://bucket:8080/path/file.txt"))
        .isEqualTo("bucket:8080");
  }

  @Test
  public void getLocationsUsedByTableWithEquivalentLocations() {
    var dataPath = IcebergTableLikeEntity.USER_SPECIFIED_WRITE_DATA_LOCATION_KEY;

    // Locations that are spelled differently but denote the same location must not cancel each
    // other out; one of them has to survive.
    Assertions.assertThat(StorageUtil.getLocationsUsedByTable("/foo", Map.of(dataPath, "/foo/")))
        .hasSize(1);
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "s3://bucket/db/t1", Map.of(dataPath, "s3://bucket/db/t1/")))
        .hasSize(1);
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "s3://bucket/db/t1", Map.of(dataPath, "s3a://bucket/db/t1")))
        .hasSize(1);
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable("file:/tmp/x", Map.of(dataPath, "file:///tmp/x")))
        .hasSize(1);

    // A genuine child is still reduced away, leaving the parent.
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "s3://bucket/db/t1", Map.of(dataPath, "s3://bucket/db/t1/data")))
        .containsExactly("s3://bucket/db/t1");
  }

  @Test
  public void getLocationsUsedByTable() {
    Assertions.assertThat(StorageUtil.getLocationsUsedByTable(null, Map.of())).isEmpty();
    Assertions.assertThat(StorageUtil.getLocationsUsedByTable("", Map.of())).isNotEmpty();
    Assertions.assertThat(StorageUtil.getLocationsUsedByTable("/foo/", Map.of())).contains("/foo/");
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "/foo/",
                Map.of(IcebergTableLikeEntity.USER_SPECIFIED_WRITE_DATA_LOCATION_KEY, "/foo/")))
        .contains("/foo/");
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "/foo/",
                Map.of(IcebergTableLikeEntity.USER_SPECIFIED_WRITE_DATA_LOCATION_KEY, "/bar/")))
        .contains("/foo/", "/bar/");
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "/foo/",
                Map.of(IcebergTableLikeEntity.USER_SPECIFIED_WRITE_DATA_LOCATION_KEY, "/foo/bar/")))
        .contains("/foo/");
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "/foo/bar/",
                Map.of(IcebergTableLikeEntity.USER_SPECIFIED_WRITE_DATA_LOCATION_KEY, "/foo/")))
        .contains("/foo/");
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "/foo/bar/",
                Map.of(IcebergTableLikeEntity.USER_SPECIFIED_WRITE_METADATA_LOCATION_KEY, "/foo/")))
        .contains("/foo/");
    Assertions.assertThat(
            StorageUtil.getLocationsUsedByTable(
                "/1/",
                Map.of(
                    IcebergTableLikeEntity.USER_SPECIFIED_WRITE_DATA_LOCATION_KEY, "/2/",
                    IcebergTableLikeEntity.USER_SPECIFIED_WRITE_METADATA_LOCATION_KEY, "/3/")))
        .contains("/1/", "/2/", "/3/");
  }
}
