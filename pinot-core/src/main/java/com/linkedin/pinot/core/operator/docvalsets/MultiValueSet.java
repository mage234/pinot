/**
 * Copyright (C) 2014-2016 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.core.operator.docvalsets;

import com.linkedin.pinot.common.data.FieldSpec.DataType;
import com.linkedin.pinot.core.common.BlockValIterator;
import com.linkedin.pinot.core.common.BlockValSet;
import com.linkedin.pinot.core.io.reader.SingleColumnMultiValueReader;
import com.linkedin.pinot.core.operator.docvaliterators.MultiValueIterator;
import com.linkedin.pinot.core.segment.index.ColumnMetadata;

public final class MultiValueSet implements BlockValSet {
  private ColumnMetadata columnMetadata;
  private SingleColumnMultiValueReader mVReader;

  public MultiValueSet(SingleColumnMultiValueReader mVReader, ColumnMetadata columnMetadata) {
    super();
    this.mVReader = mVReader;
    this.columnMetadata = columnMetadata;
  }

  @Override
  public BlockValIterator iterator() {
    return new MultiValueIterator(mVReader, columnMetadata);
  }

  @Override
  public DataType getValueType() {
    return columnMetadata.getDataType();
  }

  @Override
  public void readIntValues(int[] inDocIds, int inStartPos, int inDocIdsSize, int[] outDictionaryIds, int outStartPos) {
    throw new UnsupportedOperationException("Reading a batch of values is not supported for multi-value BlockValSet");
  }

}
