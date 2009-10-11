/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.theoryinpractice.testng.model;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;

/**
 * @author Hani Suleiman Date: Jul 28, 2005 Time: 10:44:26 PM
 */
public class TestNodeDescriptor extends NodeDescriptor<TestProxy>
{
    private final TestProxy node;

    public TestNodeDescriptor(Project project, TestProxy node, NodeDescriptor<TestProxy> parent) {
        super(project, parent);
        this.node = node;
        myName = node.getName();
    }
    
    @Override
    public boolean update() {
        return false;
    }

    @Override
    public TestProxy getElement() {
        return node;
    }

    public boolean expandOnDoubleClick() {
        return !node.isResult();
    }
}
