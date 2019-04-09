/*
 * Copyright (c)  2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.execution.string;

import io.siddhi.annotation.Example;
import io.siddhi.annotation.Extension;
import io.siddhi.annotation.Parameter;
import io.siddhi.annotation.ReturnAttribute;
import io.siddhi.annotation.util.DataType;
import io.siddhi.core.config.SiddhiAppContext;
import io.siddhi.core.event.ComplexEventChunk;
import io.siddhi.core.event.stream.StreamEvent;
import io.siddhi.core.event.stream.StreamEventCloner;
import io.siddhi.core.event.stream.populater.ComplexEventPopulater;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.exception.SiddhiAppRuntimeException;
import io.siddhi.core.executor.ConstantExpressionExecutor;
import io.siddhi.core.executor.ExpressionExecutor;
import io.siddhi.core.query.processor.Processor;
import io.siddhi.core.query.processor.stream.StreamProcessor;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.query.api.definition.AbstractDefinition;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * tokenize(sourceText, delimiter)
 * Tokenize the source String by delimiters and return as tokens.​ ​
 * Accept Type(s): (STRING, STRING)
 * Return Type(s): STRING
 */
@Extension(
        name = "tokenize",
        namespace = "str",
        description = "This function splits the input string into tokens using a given regular expression and " +
                "returns the split tokens.",
        parameters = {
                @Parameter(name = "input.string",
                        description = "The input string which needs to be split.",
                        type = {DataType.STRING}),
                @Parameter(name = "regex",
                        description = "The string value which is used to tokenize the 'input.string'.",
                        type = {DataType.STRING}),
                @Parameter(name = "distinct",
                        description = "This flag is used to return only distinct values.",
                        type = {DataType.BOOL},
                        optional = true,
                        defaultValue = "false")
        },
        returnAttributes = {
                @ReturnAttribute(
                        name = "token",
                        description = "The attribute which contains a single token.",
                        type = {DataType.STRING}
                )
        },
        examples = @Example(
                syntax = "define stream inputStream (str string);\n" +
                        "@info(name = 'query1')\n" +
                        "from inputStream#str:tokenize(str , ',')\n" +
                        "select text\n" +
                        "insert into outputStream;",
                description = "This query performs tokenization on the given string. If the str is " +
                        "\"Android,Windows8,iOS\", then the string is split into 3 events " +
                        "containing the `token` attribute values, i.e., " +
                        "`Android`, `Windows8` and `iOS`."
        )
)

public class TokenizeStreamProcessorExtension extends StreamProcessor {
    private Pattern regex;
    private boolean distinct = false;

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        while (streamEventChunk.hasNext()) {
            StreamEvent streamEvent = streamEventChunk.next();
            String event = (String) attributeExpressionExecutors[0].execute(streamEvent);
            String[] words = regex.split(event);

            // If the "distinct" flag is set true, remove all duplicate entries from the words list.
            if (this.distinct) {
                Set<String> distinctWords = new LinkedHashSet<>();
                Collections.addAll(distinctWords, words);
                words = distinctWords.toArray(new String[distinctWords.size()]);
            }

            for (String word : words) {
                Object[] data = {word};
                complexEventPopulater.populateComplexEvent(streamEvent, data);
                nextProcessor.process(streamEventChunk);
            }
        }
    }

    /**
     * The initialization method for {@link StreamProcessor}, which will be called before other methods and validate
     * the all configuration and getting the initial values.
     *
     * @param attributeExpressionExecutors are the executors of each attributes in the Function
     * @param configReader                 this hold the {@link StreamProcessor} extensions configuration reader.
     * @param siddhiAppContext             Siddhi app runtime context
     */
    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition,
                                   ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                                   SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length == 2 || attributeExpressionExecutors.length == 3) {
            if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppCreationException("Input string should be of type string. But found "
                        + attributeExpressionExecutors[0].getReturnType());
            }
            if (attributeExpressionExecutors[0] == null) {
                throw new SiddhiAppRuntimeException("Invalid input given to str:tokenize() function. " +
                        "Input.string argument cannot be null");
            }
            if (attributeExpressionExecutors[1].getReturnType() == Attribute.Type.STRING) {
                try {
                    regex = Pattern.compile((String)
                            ((ConstantExpressionExecutor) attributeExpressionExecutors[1]).getValue());
                } catch (PatternSyntaxException pse) {
                    throw new SiddhiAppValidationException("Syntax error in regular-expression pattern : " +
                            pse.getMessage());
                }
            } else {
                throw new SiddhiAppCreationException("Regex should be of type string. But found "
                        + attributeExpressionExecutors[1].getReturnType());
            }
            if (attributeExpressionExecutors[1] == null) {
                throw new SiddhiAppRuntimeException("Invalid input given to str:tokenize() function. " +
                        "Regex argument cannot be null");
            }
            if (attributeExpressionExecutors.length == 3) {
                if (attributeExpressionExecutors[2].getReturnType() == Attribute.Type.BOOL) {
                    this.distinct = (Boolean) ((ConstantExpressionExecutor) attributeExpressionExecutors[2])
                            .getValue();
                } else {
                    throw new SiddhiAppCreationException("Third attribute 'distinct' should be of type boolean. But " +
                            "found " + attributeExpressionExecutors[2].getReturnType());
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Invalid no of arguments passed to str:tokenize() function, "
                            + "required 2 or 3, but found " + attributeExpressionExecutors.length);
        }
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("token", Attribute.Type.STRING));
        return attributes;
    }

    @Override
    public void start() {
        //Do nothing
    }

    @Override
    public void stop() {
        //Do nothing
    }

    @Override
    public Map<String, Object> currentState() {
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        //Do nothing
    }
}
