package org.jahia.services.utils;

import org.drools.core.spi.KnowledgeHelper;
import org.im4java.core.*;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRValueFactoryImpl;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.image.ImageMagickImage;
import org.jahia.services.image.ImageMagickImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**

/**
 * Short description of the class
 *
 * @author faissah
 */
public class FabriceImageService extends ImageMagickImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabriceImageService.class);

    public void convertToJpeg ( AddedNodeFact imageNode, KnowledgeHelper drools) throws IOException, InterruptedException, IM4JavaException {
        final JCRNodeWrapper node = imageNode.getNode();
        ImageMagickImage image = createImage(node);
        IMOperation op = convertToPNGOperation(image);
        executeConvertCommand(op,node);
        saveOperation(node, image.getFile().getPath().replaceFirst(".png",".jpeg"));
    }

    private void executeConvertCommand(IMOperation op,JCRNodeWrapper node) {
        // create command
        ConvertCmd cmd = new ConvertCmd();
        try {
            cmd.run(op);
        } catch (IOException | InterruptedException | IM4JavaException e) {
            LOGGER.error(MessageFormat.format("Error executing Image Magick command : ${0} ${1}", cmd.getCommand().getFirst(), op.toString()), e);
        }
    }

    private ImageMagickImage createImage(JCRNodeWrapper node) {
        // create the operation, add images and operators/options
        ImageMagickImage image = null;
        try {
            image = (ImageMagickImage) super.getImage(node);
        } catch (IOException | RepositoryException e) {
            LOGGER.error("Error retrieving image for auto rotate", e);
        }
        return image;
    }
/*
    private IMOperation createAutoRotateOperation(ImageMagickImage image) {
        IMOperation op = new IMOperation();
        op.addImage(image.getFile().getPath());
        op.autoOrient();
        op.addImage(image.getFile().getPath());
        return op;
    }*/

    private IMOperation convertToPNGOperation(ImageMagickImage image) throws IM4JavaException, IOException, InterruptedException {
        String imageURL = image.getFile().getPath();
        IMOperation op = new IMOperation();
        op.addImage(imageURL);
        op.addImage(replaceLast(image.getFile().getPath(),".png",".jpeg"));
        return op;
    }

    private void saveOperation(JCRNodeWrapper node, String imagePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(replaceLast(imagePath,".png",".jpeg"));
            ValueFactory valueFactory = JCRValueFactoryImpl.getInstance();
            Binary bin = valueFactory.createBinary(fileInputStream);
            JCRNodeWrapper contentNode = node.getNode("jcr:content");
            contentNode.setProperty("jcr:data", bin);
            contentNode.setProperty("jcr:mimeType","image/jpeg");
            node.saveSession();


        } catch (RepositoryException | FileNotFoundException e) {
            LOGGER.error("Error when updating image in Jahia Node", e);
        }
    }

    private String replaceLast(String path, String oldExtension, String newExtension){
        if (path.endsWith(oldExtension)){
            String newPath= path.substring(0,path.lastIndexOf(oldExtension));
            return newPath+newExtension;
        }
        return path;
    }

    public void renameImageNode (JCRNodeWrapper node)
            throws RepositoryException {
        String mimeType=node.getFileContent().getContentType();
        String fileName = node.getName();
        if (fileName == null || fileName.isEmpty() || node.getName().lastIndexOf('.')<0) {
            return;
        }
        String nodeNameExtension = node.getName().substring(node.getName().lastIndexOf(".")+1);
        if(!node.getFileContent().getContentType().equals("image/"+nodeNameExtension)){
            fileName = replaceLast(fileName,"."+nodeNameExtension,"."+mimeType.substring(mimeType.indexOf("/")+1));
        }
        fileName = JCRContentUtils.findAvailableNodeName(node.getParent(), fileName);
        LOGGER.info("Moving node from " + node.getPath() + " to " + fileName+"to match mimetype");
        node.rename(fileName);
    }
}