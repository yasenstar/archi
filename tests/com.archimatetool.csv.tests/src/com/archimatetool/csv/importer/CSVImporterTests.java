/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.csv.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.gef.commands.CommandStack;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.archimatetool.csv.CSVParseException;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.ArchimateModelUtils;
import com.archimatetool.tests.TestUtils;

import junit.framework.JUnit4TestAdapter;


@SuppressWarnings("nls")
public class CSVImporterTests {
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(CSVImporterTests.class);
    }
    
    File testFolder = TestUtils.getLocalBundleFolder("com.archimatetool.csv.tests", "testdata");
    
    File file1 = new File(testFolder, "test1.csv");
    File file2 = new File(testFolder, "test2.csv");
    File file3 = new File(testFolder, "test3.csv");
    
    private IArchimateModel model;
    private CSVImporter importer;
    
    private String CR = System.lineSeparator();
    
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    

    @Before
    public void runOnceBeforeEachTest() {
        model = IArchimateFactory.eINSTANCE.createArchimateModel();
        model.setDefaults();
        model.setAdapter(CommandStack.class, new CommandStack());
        importer = new CSVImporter(model);
    }
    
    @Test
    public void testDoImport() throws Exception {
        importer.doImport(file1);
        
        testDoImportPart1();
        
        // Now undo it
        CommandStack stack = (CommandStack)model.getAdapter(CommandStack.class);
        stack.undo();
        
        assertEquals("", model.getName());
        assertEquals("", model.getPurpose());
        assertEquals(0, model.getProperties().size());
        
        assertEquals(0, model.getFolder(FolderType.BUSINESS).getElements().size());
        assertEquals(0, model.getFolder(FolderType.RELATIONS).getElements().size());
    }
    
    @Test
    public void testDoImportWithUpdatedElements() throws Exception {
        // Set up with original data
        importer.doImport(file1);
        
        testDoImportPart1();
        
        // Import data that is edited
        importer = new CSVImporter(model);
        importer.doImport(file2);
        
        // Ensure new concepts is empty
        assertTrue(importer.newConcepts.isEmpty());

        // Ensure new properties is empty
        assertTrue(importer.newProperties.isEmpty());
        
        // Model information
        assertEquals("Test Model changed", model.getName());
        assertEquals("Model Documentation Changed", model.getPurpose());
        assertEquals(2, model.getProperties().size());
        
        IArchimateElement element = (IArchimateElement)ArchimateModelUtils.getObjectByID(model, "f00aa5b4");
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessActor(), element.eClass());
        assertEquals("Name changed", element.getName());
        assertEquals("This is the Business Actor" + CR + "Documentation" + CR + "Here \"\"" + CR, element.getDocumentation());
        assertEquals(4, element.getProperties().size());
        
        element = (IArchimateElement)ArchimateModelUtils.getObjectByID(model, "d9fe8c17");
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessInterface(), element.eClass());
        assertEquals("Business Interface", element.getName());
        assertEquals("", element.getDocumentation());
        assertEquals(0, element.getProperties().size());
        
        IArchimateRelationship relation = (IArchimateRelationship)ArchimateModelUtils.getObjectByID(model, "cdbfc933");
        assertEquals(IArchimatePackage.eINSTANCE.getAssignmentRelationship(), relation.eClass());
        assertEquals("Assignment relation changed", relation.getName());
        assertEquals("Assignment documentation changed", relation.getDocumentation());
        assertEquals(0, relation.getProperties().size());
        
        relation = (IArchimateRelationship)ArchimateModelUtils.getObjectByID(model, "5854f8a3");
        assertEquals(IArchimatePackage.eINSTANCE.getCompositionRelationship(), relation.eClass());
        assertEquals("5854f8a3", relation.getId());
        assertEquals("Compo", relation.getName());
        assertEquals("Here it is" + CR + "again" + CR + CR + CR, relation.getDocumentation());

        assertEquals(1, relation.getProperties().size());
        IProperty property = relation.getProperties().get(0);
        assertEquals("This", property.getKey());
        assertEquals("value changes", property.getValue());
    }
    
    private void testDoImportPart1() {
        // Model information
        String modelID = model.getId(); // This should not be changed
        assertEquals(modelID, model.getId());
        assertEquals("Test Model", model.getName());
        assertEquals("This is the Purpose of the Model." + CR + "It has a line break and \"some\" quotes." + CR, model.getPurpose());
        assertEquals(2, model.getProperties().size());

        // Elements
        assertEquals(3, model.getFolder(FolderType.BUSINESS).getElements().size());
        
        // Relations
        assertEquals(2, model.getFolder(FolderType.RELATIONS).getElements().size());
        
        // Ensure updated concepts is empty
        assertTrue(importer.updatedConcepts.isEmpty());
    }
    
    @Test
    public void testImportModelElements() throws Exception {
        importer.doImport(file1);
        
        assertEquals(5, importer.newConcepts.size());
        
        IArchimateConcept concept = importer.newConcepts.get("f00aa5b4");
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessActor(), concept.eClass());
        assertEquals("f00aa5b4", concept.getId());
        assertEquals("Business Actor", concept.getName());
        assertEquals("This is the Business Actor" + CR + "Documentation" + CR + "Here \"\"" + CR, concept.getDocumentation());
        
        concept = importer.newConcepts.get("d9fe8c17");
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessInterface(), concept.eClass());
        assertEquals("d9fe8c17", concept.getId());
        assertEquals("Business Interface", concept.getName());
        assertEquals("", concept.getDocumentation());
        
        concept = importer.newConcepts.get("f6a18059");
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessRole(), concept.eClass());
        assertEquals("f6a18059", concept.getId());
        assertEquals("Business Role", concept.getName());
        assertEquals("Some more docs" + CR + "Here" + CR, concept.getDocumentation());
    }

    @Test
    public void testImportElementsAndRelationsWithNoIDsHaveIDsGenerated() throws Exception {
        importer.doImport(file3);
        
        assertEquals(6, importer.newConcepts.size());
        for(String id : importer.newConcepts.keySet()) {
            assertTrue(StringUtils.isSet(id));
        }
    }

    @Test
    public void testImportRelations() throws Exception {
        importer.doImport(file1);
        
        assertEquals(5, importer.newConcepts.size());
        
        IArchimateRelationship relation = (IArchimateRelationship)importer.newConcepts.get("cdbfc933");
        assertEquals(IArchimatePackage.eINSTANCE.getAssignmentRelationship(), relation.eClass());
        assertEquals("cdbfc933", relation.getId());
        assertEquals("Assignment relation", relation.getName());
        assertEquals("Assignment documentation" + CR + "Is here \"hello\"", relation.getDocumentation());
        IArchimateConcept source = relation.getSource();
        assertNotNull(source);
        assertEquals("f00aa5b4", source.getId());
        IArchimateConcept target = relation.getTarget();
        assertNotNull(target);
        assertEquals("f6a18059", target.getId());
        
        relation = (IArchimateRelationship)importer.newConcepts.get("5854f8a3");
        assertEquals(IArchimatePackage.eINSTANCE.getCompositionRelationship(), relation.eClass());
        assertEquals("5854f8a3", relation.getId());
        assertEquals("Compo", relation.getName());
        assertEquals("Here it is" + CR+ "again" + CR + CR + CR, relation.getDocumentation());
        source = relation.getSource();
        assertNotNull(source);
        assertEquals("f00aa5b4", source.getId());
        target = relation.getTarget();
        assertNotNull(target);
        assertEquals("d9fe8c17", target.getId());
    }
    
    @Test
    public void testImportProperties() throws Exception {
        importer.doImport(file1);
        assertEquals(7, importer.newProperties.size());
    }

    @Test
    public void testNormalise() {
        assertEquals("", importer.normalise(null));
        assertEquals("ok here", importer.normalise("ok here"));
        assertEquals("tab here", importer.normalise("tab\there"));
        assertEquals("line feed", importer.normalise("line\rfeed"));
        assertEquals("line feed", importer.normalise("line\nfeed"));
        assertEquals("line feed", importer.normalise("line\r\nfeed"));
    }
    
    @Test
    public void testCheckIDForInvalidCharacters_Fail() {
        String[] testStrings = {
                "&", " ", "*", "$", "#"
        };
        
        for(String s : testStrings) {
            try {
                importer.checkIDForInvalidCharacters(s);
                fail("Should throw CSVParseException");
            }
            catch(CSVParseException ex) {
                continue;
            }
        }
    }
    
    @Test
    public void testCheckIDForInvalidCharacters_Pass() throws Exception {
        String[] testStrings = {
                "f00aa5b4", "123Za", "_-123uioP09..-_"
        };
        
        for(String s : testStrings) {
            importer.checkIDForInvalidCharacters(s);
        }
    }

    @Test
    public void testFindArchimateConceptInModel() throws Exception {
        importer.doImport(file1);
        assertNotNull(importer.findArchimateConceptInModel("f00aa5b4", IArchimatePackage.eINSTANCE.getBusinessActor()));
    }

    @Test
    public void testFindArchimateConceptInModel_DifferentClass() throws Exception {
        expectedEx.expect(CSVParseException.class);
        expectedEx.expectMessage("Found element with same id but different class: f6a18059");
        
        importer.doImport(file1);
        importer.findArchimateConceptInModel("f6a18059", IArchimatePackage.eINSTANCE.getBusinessActor());
    }
    
    @Test
    public void testFindReferencedConcept() throws Exception {
        importer.doImport(file1);
        assertNotNull(importer.findReferencedConcept("f6a18059"));
    }
    
    @Test
    public void testFindReferencedConcept_IsRelationship() throws Exception {
        importer.doImport(file1);
        IArchimateConcept concept = importer.findReferencedConcept("5854f8a3");
        assertTrue(concept instanceof IArchimateRelationship);
    }

    @Test(expected=CSVParseException.class)
    public void testFindReferencedConcept_NotFound() throws Exception {
        importer.doImport(file1);
        importer.findReferencedConcept("someid");
    }

    @Test(expected=CSVParseException.class)
    public void testFindReferencedConcept_Null() throws CSVParseException {
        importer.findReferencedConcept(null);
    }
   
    @Test
    public void testIsArchimateConceptEClass() {
        assertFalse(importer.isArchimateConceptEClass(null));
        assertFalse(importer.isArchimateConceptEClass(IArchimatePackage.eINSTANCE.getFolder()));
        assertTrue(importer.isArchimateConceptEClass(IArchimatePackage.eINSTANCE.getAccessRelationship()));
        assertTrue(importer.isArchimateConceptEClass(IArchimatePackage.eINSTANCE.getBusinessActor()));
    }

    @Test
    public void testIsArchimateElementEClass() {
        assertFalse(importer.isArchimateElementEClass(null));
        assertFalse(importer.isArchimateConceptEClass(IArchimatePackage.eINSTANCE.getFolder()));
        assertFalse(importer.isArchimateElementEClass(IArchimatePackage.eINSTANCE.getAccessRelationship()));
        assertTrue(importer.isArchimateElementEClass(IArchimatePackage.eINSTANCE.getBusinessActor()));
    }
   
    @Test
    public void testIsArchimateRelationshipEClass() {
        assertFalse(importer.isArchimateRelationshipEClass(null));
        assertFalse(importer.isArchimateConceptEClass(IArchimatePackage.eINSTANCE.getFolder()));
        assertFalse(importer.isArchimateRelationshipEClass(IArchimatePackage.eINSTANCE.getBusinessActor()));
        assertTrue(importer.isArchimateRelationshipEClass(IArchimatePackage.eINSTANCE.getAccessRelationship()));
    }
    
    @Test
    public void testGetProperty() {
        IArchimateElement element = IArchimateFactory.eINSTANCE.createBusinessActor();
        IProperty property = IArchimateFactory.eINSTANCE.createProperty();
        property.setKey("key");
        property.setValue("value");
        element.getProperties().add(property);
        
        assertEquals(property, importer.getProperty(element, "key"));
        assertNull(importer.getProperty(element, "key2"));
    }
}
