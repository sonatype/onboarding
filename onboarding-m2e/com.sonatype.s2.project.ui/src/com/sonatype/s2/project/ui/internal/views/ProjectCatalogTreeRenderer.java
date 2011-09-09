/**
 * Copyright (c) 2008-2010 Sonatype, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 */
package com.sonatype.s2.project.ui.internal.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.model.IS2ProjectFilter;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.ui.internal.ProjectData;

public class ProjectCatalogTreeRenderer
    extends LabelProvider
    implements Listener
{
    private static final Logger log = LoggerFactory.getLogger( ProjectCatalogTreeRenderer.class );

    private static final int HORIZONTAL_MARGIN = 6;

    private static final int VERTICAL_MARGIN = 3;

    private static final int IMAGE_BEST_SIZE = 48;

    private static final int RIGHT_MARGIN = 24;

    private static final int TEXT_MARGIN = IMAGE_BEST_SIZE + HORIZONTAL_MARGIN * 2;

    private static final int ITEM_HEIGHT = VERTICAL_MARGIN + IMAGE_BEST_SIZE + VERTICAL_MARGIN;

    private TreeViewer viewer;

    private Tree tree;

    private int width;

    private Map<String, ProjectData> projects;

    private IS2ProjectFilter projectFilter;

    public ProjectCatalogTreeRenderer( TreeViewer viewer, IS2ProjectFilter projectFilter )
    {
        this.viewer = viewer;
        this.projectFilter = projectFilter;

        projects = new HashMap<String, ProjectData>();

        viewer.setLabelProvider( this );

        tree = viewer.getTree();

        tree.addListener( SWT.Resize, this );
        tree.addListener( SWT.EraseItem, this );
        tree.addListener( SWT.MeasureItem, this );
        tree.addListener( SWT.PaintItem, this );
    }

    public void clear()
    {
        projects.clear();
    }

    public ProjectData getProjectData( IS2ProjectCatalogEntry entry )
    {
        return projects.get( entry.getEffectiveDescriptorUrl() );
    }

    public String getText( Object obj )
    {
        if ( obj instanceof IS2ProjectCatalog )
        {
            String s = ( (IS2ProjectCatalog) obj ).getName();
            return s == null || s.length() == 0 ? ( (IS2ProjectCatalog) obj ).getUrl() : s;
        }
        else if ( obj instanceof IS2ProjectCatalogEntry )
        {
            return ( (IS2ProjectCatalogEntry) obj ).getName();
        }
        else if ( obj instanceof IS2Module )
        {
            return ( (IS2Module) obj ).getName();
        }
        return String.valueOf( obj );
    }

    public Job loadEntries( final List<IS2ProjectCatalogEntry> entries )
    {
        Job job = new Job( Messages.projectData_jobs_loadingProjects )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                List<IStatus> errors = new ArrayList<IStatus>();
                final List<ProjectData> loaded = new ArrayList<ProjectData>();
                for ( IS2ProjectCatalogEntry entry : entries )
                {
                    try
                    {
                        ProjectData data = new ProjectData( entry );
                        data.load( monitor, true );
                        if ( projectFilter == null || projectFilter.accept( data.getProject() ) )
                        {
                            loaded.add( data );
                        }
                        else
                        {
                            log.debug( "Project '{}' does not match provided filter.", data.getProject().getName() );
                        }
                    }
                    catch ( CoreException e )
                    {
                        errors.add( e.getStatus() );
                    }
                }

                Display.getDefault().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        for ( ProjectData data : loaded )
                        {
                            projects.put( data.getUrl(), data );
                            viewer.update( data.getEntry(), null );
                        }
                    }
                } );

                return errors.isEmpty() ? Status.OK_STATUS
                                : new MultiStatus( Activator.PLUGIN_ID, 0,
                                                   errors.toArray( new IStatus[errors.size()] ),
                                                   Messages.projectData_errors_errorLoadingProjects, null );
            }
        };
        job.schedule();
        return job;
    }

    protected void drawImage( GC gc, Image image, int x, int y )
    {
        Rectangle r = image.getBounds();
        int width = IMAGE_BEST_SIZE;
        int height = IMAGE_BEST_SIZE;
        int imageMax = Math.max( r.height, r.width );

        if ( imageMax <= IMAGE_BEST_SIZE / 2 || imageMax > IMAGE_BEST_SIZE )
        {
            // if the image is too small or too large, scale it
            width = ( IMAGE_BEST_SIZE * r.width ) / imageMax;
            height = ( IMAGE_BEST_SIZE * r.height ) / imageMax;
        }
        else if ( imageMax != IMAGE_BEST_SIZE )
        {
            // if the image is just a bit smaller, center it horizontally
            x += ( IMAGE_BEST_SIZE - r.width ) / 2;
        }

        gc.drawImage( image, 0, 0, r.width, r.height, x + HORIZONTAL_MARGIN, y + VERTICAL_MARGIN, width, height );
    }

    protected void drawTitle( GC gc, String text, int x, int y )
    {
        Font oldFont = gc.getFont();
        Font newFont = null;

        FontData[] fontData = oldFont.getFontData();
        if ( fontData.length > 0 )
        {
            for ( FontData data : fontData )
            {
                data.setStyle( data.getStyle() | SWT.BOLD );
            }
            newFont = new Font( gc.getDevice(), fontData );
            gc.setFont( newFont );
        }

        gc.drawString( text, x + TEXT_MARGIN, y + VERTICAL_MARGIN, true );

        if ( newFont != null )
        {
            newFont.dispose();
            gc.setFont( oldFont );
        }
    }

    protected void drawDescription( GC gc, String text, int x, int y )
    {
        Font oldFont = gc.getFont();
        y += VERTICAL_MARGIN * 2 + gc.getFontMetrics().getHeight();

        FontData[] fontData = oldFont.getFontData();
        for ( FontData data : fontData )
        {
            data.setHeight( data.getHeight() - 1 );
        }
        Font newFont = new Font( gc.getDevice(), fontData );
        gc.setFont( newFont );

        TextLayout textLayout = new TextLayout( gc.getDevice() );
        textLayout.setFont( gc.getFont() );
        textLayout.setText( text );
        textLayout.setWidth( width - x - TEXT_MARGIN - RIGHT_MARGIN );

        if ( !Platform.WS_GTK.equals( SWT.getPlatform() ) )
        {
            for ( int i = 0; textLayout.getLineCount() > 2 && i < 3; i++ )
            {
                // Windows and Mac do not allow unequal item height, so we'll draw the first two lines
                int[] offsets = textLayout.getLineOffsets();
                textLayout.setText( text.substring( 0, offsets[2] - 1 ) + "..." );
            }
        }

        textLayout.draw( gc, x + TEXT_MARGIN, y );
        textLayout.dispose();

        newFont.dispose();
        gc.setFont( oldFont );
    }

    public void handleEvent( Event event )
    {
        switch ( event.type )
        {
            case SWT.MeasureItem:
                measureItem( event );
                break;
            case SWT.EraseItem:
                eraseItem( event );
                break;
            case SWT.PaintItem:
                paintItem( event );
                break;
            case SWT.Resize:
                if ( event.widget == tree )
                {
                    width = tree.getClientArea().width;
                }
                break;
        }
    }

    protected void eraseItem( Event event )
    {
        event.detail &= ~SWT.FOREGROUND;
    }

    protected void measureItem( Event event )
    {
        if ( event.gc == null || event.item.getData() == null )
        {
            return;
        }

        event.width = width - ( event.x == 0 ? TEXT_MARGIN : event.x ) - RIGHT_MARGIN;
        event.height = ITEM_HEIGHT;
        int height = 0;

        if ( !Platform.WS_GTK.equals( SWT.getPlatform() ) )
        {
            // Windows and Mac do not allow unequal item height
            height = event.gc.getFontMetrics().getHeight() * 3 + VERTICAL_MARGIN * 2;
        }
        else if ( event.item.getData() instanceof IS2ProjectCatalogEntry )
        {
            ProjectData projectData = getProjectData( (IS2ProjectCatalogEntry) event.item.getData() );
            if ( projectData != null && projectData.getProject().getDescription() != null )
            {
                FontData[] fontData = event.gc.getFont().getFontData();
                for ( FontData data : fontData )
                {
                    data.setHeight( data.getHeight() - 1 );
                }
                Font newFont = new Font( event.gc.getDevice(), fontData );

                TextLayout textLayout = new TextLayout( event.gc.getDevice() );
                textLayout.setFont( newFont );
                textLayout.setText( projectData.getProject().getDescription() );
                textLayout.setWidth( event.width - TEXT_MARGIN - RIGHT_MARGIN );

                height = event.gc.getFontMetrics().getHeight() + textLayout.getBounds().height + VERTICAL_MARGIN * 3;

                textLayout.dispose();
                newFont.dispose();
            }
        }

        if ( height > event.height )
        {
            event.height = height;
        }
    }

    protected void paintItem( Event event )
    {
        if ( event.item.getData() instanceof IS2ProjectCatalogEntry )
        {
            IS2ProjectCatalogEntry entry = (IS2ProjectCatalogEntry) event.item.getData();
            Image image = Images.DEFAULT_PROJECT_IMAGE;
            String titleText = entry.getName();
            String descriptionText = "";

            ProjectData projectData = getProjectData( entry );
            if ( projectData != null )
            {
                titleText = projectData.getProject().getName();
                if ( projectData.getImage() != null )
                {
                    image = projectData.getImage();
                }
                if ( projectData.getProject().getDescription() != null )
                {
                    descriptionText = projectData.getProject().getDescription();
                }
            }
            else
            {
                descriptionText = entry.getEffectiveDescriptorUrl();
            }

            drawItem( event, image, titleText, descriptionText );
        }
        else
        {
            drawItem( event, Images.DEFAULT_DOCUMENT_IMAGE, "", getText( event.item.getData() ) );
        }
    }

    protected void drawItem( Event event, Image image, String titleText, String descriptionText )
    {
        Rectangle r = ( (TreeItem) event.item ).getBounds();
        int x = r.x == 0 ? event.x : r.x;
        int y = r.y;

        drawImage( event.gc, image, x, y );
        drawTitle( event.gc, titleText, x, y );
        drawDescription( event.gc, descriptionText, x, y );
    }
}
