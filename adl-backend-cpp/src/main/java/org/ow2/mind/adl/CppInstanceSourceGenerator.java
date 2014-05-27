/**
 * Copyright (C) 2014 Schneider-Electric
 *
 * This file is part of "Mind Compiler" is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact: mind@ow2.org
 *
 * Authors: Stephane Seyvoz (Assystem)
 * Contributors: 
 */

package org.ow2.mind.adl;

import static org.ow2.mind.NameHelper.toValidName;
import static org.ow2.mind.PathHelper.fullyQualifiedNameToPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.antlr.stringtemplate.StringTemplate;
import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.CompilerError;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.SourceFileWriter;
import org.ow2.mind.adl.graph.ComponentGraph;
import org.ow2.mind.io.IOErrors;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * {@link InstanceSourceGenerator} component that generated {@value #FILE_EXT}
 * files using the {@value #DEFAULT_TEMPLATE} template.
 */
public class CppInstanceSourceGenerator extends AbstractSourceGenerator
    implements
      InstanceSourceGenerator {

  // The cpp-instance logger
  protected static Logger       cppInstanceLogger = FractalADLLogManager
                                                      .getLogger("cpp-instance");

  /** The name to be used to inject the templateGroupName used by this class. */
  public static final String    TEMPLATE_NAME     = "cpp.instances";

  /** The default templateGroupName used by this class. */
  public static final String    DEFAULT_TEMPLATE  = "st.cpp.instances.Component";

  protected static final String FILE_EXT          = ".cpp";

  @Inject
  protected CppInstanceSourceGenerator(
      @Named(TEMPLATE_NAME) final String templateGroupName) {
    super(templateGroupName);
  }

  // ---------------------------------------------------------------------------
  // public static methods
  // ---------------------------------------------------------------------------

  /**
   * A static method that returns the name of the file that is generated by this
   * component for the given {@link InstancesDescriptor};
   * 
   * @param instanceDesc an {@link InstancesDescriptor} node.
   * @return the name of the file that is generated by this component for the
   *         given {@link Definition};
   */
  public static String getInstancesFileName(
      final InstancesDescriptor instanceDesc) {
    String outputFileName = toValidName(instanceDesc.topLevelDefinition
        .getName());
    outputFileName += "_"
        + toValidName(instanceDesc.instanceDefinition.getName()).replace('.',
            '_') + "_instances";

    outputFileName = fullyQualifiedNameToPath(outputFileName, FILE_EXT);
    return outputFileName;
  }

  // ---------------------------------------------------------------------------
  // Implementation of the InstanceSourceGenerator interface
  // ---------------------------------------------------------------------------

  public void visit(final InstancesDescriptor instanceDesc,
      final Map<Object, Object> context) throws ADLException {
    final File outputFile = outputFileLocatorItf.getCSourceOutputFile(
        getInstancesFileName(instanceDesc), context);

    if (regenerate(outputFile, instanceDesc.topLevelDefinition, context)) {

      final StringTemplate st;
      if (instanceDesc.topLevelDefinition == instanceDesc.instanceDefinition) {
        st = getInstanceOf("TopLevelInstances");

        st.setAttribute("topLevelDefinition", instanceDesc.topLevelDefinition);
        st.setAttribute("instances", instanceDesc.instances);

        final Set<Definition> definitions = new LinkedHashSet<Definition>();
        for (final ComponentGraph instance : instanceDesc.instances) {
          addDefinitions(instance, definitions);
        }

        st.setAttribute("definitions", definitions);
      } else {
        st = getInstanceOf("ComponentInstances");

        st.setAttribute("topLevelDefinition", instanceDesc.topLevelDefinition);
        st.setAttribute("definition", instanceDesc.instanceDefinition);
        st.setAttribute("instances", instanceDesc.instances);

        // navigation to build name from root
        decorateWithNameFromRoot(instanceDesc.instances);
      }

      try {
        SourceFileWriter.writeToFile(outputFile, st.toString());
      } catch (final IOException e) {
        throw new CompilerError(IOErrors.WRITE_ERROR, e,
            outputFile.getAbsolutePath());
      }
    }
  }

  /**
   * This method allows to build an instance name path from the root component,
   * navigating upwards the tree.
   * 
   * @param instances
   */
  private void decorateWithNameFromRoot(
      final Collection<ComponentGraph> instances) {

    for (final ComponentGraph instance : instances) {
      final ComponentGraph[] parents = instance.getParents();
      // Assume there is only one parent ?

      if (parents.length == 1) {
        final Collection<ComponentGraph> parentGraphInList = new ArrayList<ComponentGraph>();
        parentGraphInList.add(parents[0]);

        // go upwards and decorate with name
        decorateWithNameFromRoot(parentGraphInList);

        // get resulting name from going upwards
        String previousNameInParent = (String) instance
            .getDecoration("nameInParent");

        // concatenate the new name
        if (previousNameInParent == null) previousNameInParent = "";

        /* _mind_ prefix at every level to avoid name clashes */
        instance.setDecoration("nameInParent", previousNameInParent + "."
            + "_mind_" + instance.getNameInParent(parents[0]));

      } else if (parents.length > 1)
        // TODO: change with error log to throw a fatal exception
        cppInstanceLogger
            .severe("More than one parent encountered for instance "
                + instance.getDecoration("instance-name") + " !");
    }

  }

  protected void addDefinitions(final ComponentGraph graph,
      final Set<Definition> definitions) {
    definitions.add(graph.getDefinition());

    for (final ComponentGraph subComp : graph.getSubComponents()) {
      addDefinitions(subComp, definitions);
    }
  }

}
