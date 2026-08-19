import { BookOpen } from 'lucide-react';
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  useSidebar,
} from '../ui/sidebar';
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemMedia,
  ItemTitle,
} from '../ui/item';
import { Badge } from '../ui/badge';
import { formatLabDeadlineMeta } from '../../theme/statusClasses';

function urgencyBadge(urgencyState) {
  switch (urgencyState) {
    case 'WARNING':
      return { variant: 'warning', label: '3 days' };
    case 'URGENT':
      return { variant: 'destructive', label: '1 day' };
    case 'EXPIRED':
      return { variant: 'secondary', label: 'Expired' };
    default:
      return null;
  }
}

export default function StudentLabSidebar({
  labs = [],
  selectedLabId = null,
  onSelectLab = () => {},
}) {
  const { isMobile, setOpenMobile } = useSidebar();

  function handleSelect(labId) {
    onSelectLab(labId);
    if (isMobile) setOpenMobile(false);
  }

  return (
    <Sidebar>
      <SidebarHeader>
        <div className="px-1 py-0.5">
          <p className="text-sm font-semibold text-foreground">Labs</p>
          <p className="text-xs text-foreground-muted">Choose an assignment</p>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Assignments</SidebarGroupLabel>
          <SidebarGroupContent>
            {labs.length === 0 ? (
              <p className="px-2 py-6 text-center text-sm text-foreground-muted">
                No labs available
              </p>
            ) : (
              <ItemGroup>
                {labs.map((lab) => {
                  const selected = String(lab.id) === String(selectedLabId);
                  const badge = urgencyBadge(lab.urgencyState);
                  return (
                    <Item
                      key={lab.id}
                      as="button"
                      type="button"
                      size="sm"
                      variant={selected ? 'muted' : 'default'}
                      aria-current={selected ? 'true' : undefined}
                      onClick={() => handleSelect(lab.id)}
                      className={selected ? 'ring-1 ring-primary' : ''}
                    >
                      <ItemMedia variant="icon">
                        <BookOpen className="h-4 w-4 text-primary" />
                      </ItemMedia>
                      <ItemContent>
                        <ItemTitle className={selected ? 'text-primary-text' : undefined}>
                          {lab.name}
                        </ItemTitle>
                        <ItemDescription>{formatLabDeadlineMeta(lab)}</ItemDescription>
                      </ItemContent>
                      {badge && (
                        <ItemActions>
                          <Badge variant={badge.variant}>{badge.label}</Badge>
                        </ItemActions>
                      )}
                    </Item>
                  );
                })}
              </ItemGroup>
            )}
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
